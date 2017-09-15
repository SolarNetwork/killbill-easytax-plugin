/*  Copyright 2017 SolarNetwork Foundation
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.killbill.billing.plugin.easytax.core;

import static java.util.Collections.singleton;
import static org.killbill.billing.plugin.easytax.EasyTaxTestUtils.assertDateTimeEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.billing.plugin.easytax.api.EasyTaxDao;
import org.killbill.billing.security.Permission;
import org.killbill.billing.security.api.SecurityApi;
import org.killbill.billing.tenant.api.Tenant;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.clock.Clock;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;

/**
 * Test cases for the {@link EasyTaxServlet} class.
 * 
 * @author matt
 */
public class EasyTaxServletTests {

    private static final String TEST_PASSWORD = "password";
    private static final String TEST_USER = "admin";
    private static final Set<Permission> TEST_PERMISSIONS = singleton(
            Permission.CATALOG_CAN_UPLOAD);

    private EasyTaxDao dao;
    private Clock clock;
    private SecurityApi securityApi;
    private HttpServletRequest req;
    private HttpServletResponse res;
    private EasyTaxServlet servlet;
    private DateTime now;
    private UUID tenantId;
    private Tenant tenant;

    @BeforeMethod
    public void setup() {
        now = new DateTime(DateTimeZone.UTC);
        tenantId = UUID.randomUUID();
        tenant = mock(Tenant.class);
        clock = mock(Clock.class);
        dao = mock(EasyTaxDao.class);
        securityApi = mock(SecurityApi.class);
        servlet = new EasyTaxServlet(dao, clock, securityApi);
        req = mock(HttpServletRequest.class);
        res = mock(HttpServletResponse.class);
    }

    private void givenPermissions(String username, String password, Set<Permission> permissions) {
        String credentials = username + ":" + password;
        try {
            String authHeaderValue = "Basic "
                    + Base64.getEncoder().encodeToString(credentials.getBytes("UTF-8"));
            given(req.getHeader("Authorization")).willReturn(authHeaderValue);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        given(securityApi.isSubjectAuthenticated()).willReturn(true);
        given(securityApi
                .getCurrentUserPermissions(Matchers.argThat(new ArgumentMatcher<TenantContext>() {

                    @Override
                    public boolean matches(Object arg) {
                        return (arg instanceof TenantContext
                                && tenantId.equals(((TenantContext) arg).getTenantId()));
                    }

                }))).willReturn(permissions);
    }

    private void thenAuthenticatedAs(String username, String password) {
        then(securityApi).should().login(username, password);
    }

    private ByteArrayOutputStream givenTenantlessServletCall(String method, String path)
            throws IOException {
        return givenTenantlessServletCall(method, path, null, 0, null);
    }

    private ByteArrayOutputStream givenTenantlessServletCall(String method, String path,
            InputStream content, int contentLength, String contentType) throws IOException {
        return givenServletCall(null, method, path, content, contentLength, contentType);
    }

    private ByteArrayOutputStream givenDefaultServletCall(String method, String path)
            throws IOException {
        return givenDefaultServletCall(method, path, null, 0, null);
    }

    private ByteArrayOutputStream givenDefaultServletCall(String method, String path,
            InputStream content, int contentLength, String contentType) throws IOException {
        return givenServletCall(tenantId, method, path, content, contentLength, contentType);
    }

    private ByteArrayOutputStream givenServletCall(UUID tenantId, String method, String path,
            InputStream content, int contentLength, String contentType) throws IOException {
        if (tenantId != null) {
            given(tenant.getId()).willReturn(tenantId);
            given(req.getAttribute("killbill_tenant")).willReturn(tenant);
        }
        given(clock.getUTCNow()).willReturn(now);
        given(req.getMethod()).willReturn(method);
        given(req.getPathInfo()).willReturn(path);
        if (contentType != null) {
            given(req.getContentType()).willReturn(contentType);
        }
        if (content != null) {
            given(req.getContentLength()).willReturn(contentLength);
            given(req.getContentLengthLong()).willReturn(Long.valueOf(contentLength));
            ServletInputStream in = new ServletInputStream() {

                private int read = 0;

                @Override
                public int read() throws IOException {
                    int result = content.read();
                    if (result >= 0) {
                        read++;
                    }
                    return result;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // not supported
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public boolean isFinished() {
                    return (read >= contentLength);
                }
            };
            given(req.getInputStream()).willReturn(in);
        }
        ByteArrayOutputStream byos = new ByteArrayOutputStream();
        ServletOutputStream out = new ServletOutputStream() {

            @Override
            public void write(int b) throws IOException {
                byos.write(b);
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                // ignore
            }

            @Override
            public boolean isReady() {
                return true;
            }
        };
        given(res.getOutputStream()).willReturn(out);
        return byos;
    }

    private void thenDefaultOkJsonResponse() {
        thenDefaultOkResponse(EasyTaxServlet.APPLICATION_JSON_UTF8);
    }

    private void thenDefaultOkResponse() {
        thenDefaultOkResponse(null);
    }

    private void thenDefaultOkResponse(String contentType) {
        thenDefaultResponse(200, contentType);
    }

    private void thenDefaultResponse(int status, String contentType) {
        then(res).should().setStatus(status);
        if (contentType != null) {
            then(res).should().setContentType(contentType);
        }
    }

    private void thenErrorResponse(int status, String msg) throws IOException {
        then(res).should().sendError(status, msg);
    }

    @Test(groups = "fast")
    public void getUnknownPath() throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("GET", "/shazam");

        // when
        servlet.service(req, res);

        // then
        thenErrorResponse(404, "Resource /shazam not found");

        assertEquals(byos.size(), 0, "Response body content");
    }

    @Test(groups = "fast")
    public void postUnknownPath() throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("POST", "/shazam");
        givenPermissions(TEST_USER, TEST_PASSWORD, TEST_PERMISSIONS);

        // when
        servlet.service(req, res);

        // then
        thenAuthenticatedAs(TEST_USER, TEST_PASSWORD);
        thenErrorResponse(404, "Resource /shazam not found");

        assertEquals(byos.size(), 0, "Response body content");
    }

    @Test(groups = "fast")
    public void deleteUnknownPath() throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("DELETE", "/shazam");
        givenPermissions(TEST_USER, TEST_PASSWORD, TEST_PERMISSIONS);

        // when
        servlet.service(req, res);

        // then
        thenAuthenticatedAs(TEST_USER, TEST_PASSWORD);
        thenErrorResponse(404, "Resource /shazam not found");

        assertEquals(byos.size(), 0, "Response body content");
    }

    @Test(groups = "fast")
    public void getAllTaxCodes() throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("GET", "/taxCodes");

        List<EasyTaxTaxCode> taxCodes = Arrays
                .asList(new EasyTaxTaxCode(UUID.randomUUID().toString()));
        given(dao.getTaxCodes(tenantId, null, null, null, null)).willReturn(taxCodes);

        // when
        servlet.service(req, res);

        // then
        thenDefaultOkJsonResponse();

        assertEquals(byos.toString("UTF-8"),
                "[{\"tax_code\":\"" + taxCodes.get(0).getTaxCode() + "\"}]",
                "Response body content");
    }

    @Test(groups = "fast")
    public void getAllTaxCodesTenantless() throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenTenantlessServletCall("GET", "/taxCodes");

        // when
        servlet.service(req, res);

        // then
        thenErrorResponse(404, "No tenant specified");

        assertEquals(byos.size(), 0, "Response body content");
    }

    @Test(groups = "fast")
    public void getAllTaxCodesDatesFormattedToUTC()
            throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("GET", "/taxCodes");

        EasyTaxTaxCode taxCode = new EasyTaxTaxCode(UUID.randomUUID().toString());
        taxCode.setCreatedDate(
                new DateTime(2017, 1, 1, 12, 13, 14, DateTimeZone.forOffsetHours(12)));
        taxCode.setValidFromDate(new DateTime(2010, 1, 1, 0, 0, 0, DateTimeZone.UTC));
        taxCode.setValidToDate(new DateTime(2099, 1, 1, 0, 0, 0, DateTimeZone.UTC));
        List<EasyTaxTaxCode> taxCodes = Collections.singletonList(taxCode);
        given(dao.getTaxCodes(tenantId, null, null, null, null)).willReturn(taxCodes);

        // when
        servlet.service(req, res);

        // then
        thenDefaultOkJsonResponse();

        assertEquals(byos.toString("UTF-8"),
                "[{\"created_date\":\"2017-01-01T00:13:14.000Z\",\"tax_code\":\""
                        + taxCode.getTaxCode().toString()
                        + "\",\"valid_from_date\":\"2010-01-01T00:00:00.000Z\""
                        + ",\"valid_to_date\":\"2099-01-01T00:00:00.000Z\"}]",
                "Response body content");
    }

    @Test(groups = "fast")
    public void getAllTaxCodesRateFormattedAsString()
            throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("GET", "/taxCodes");

        EasyTaxTaxCode taxCode = new EasyTaxTaxCode(UUID.randomUUID().toString());
        taxCode.setTaxRate(new BigDecimal("0.15"));
        List<EasyTaxTaxCode> taxCodes = Collections.singletonList(taxCode);
        given(dao.getTaxCodes(tenantId, null, null, null, null)).willReturn(taxCodes);

        // when
        servlet.service(req, res);

        // then
        thenDefaultOkJsonResponse();

        assertEquals(byos.toString("UTF-8"), "[{\"tax_code\":\"" + taxCode.getTaxCode().toString()
                + "\",\"tax_rate\":\"0.15\"}]", "Response body content");
    }

    @Test(groups = "fast")
    public void getTaxCodesForTaxZone() throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("GET", "/taxCodes/NZ");

        List<EasyTaxTaxCode> taxCodes = Arrays
                .asList(new EasyTaxTaxCode(UUID.randomUUID().toString()));
        given(dao.getTaxCodes(tenantId, "NZ", null, null, null)).willReturn(taxCodes);

        // when
        servlet.service(req, res);

        // then
        thenDefaultOkJsonResponse();

        assertEquals(byos.toString("UTF-8"),
                "[{\"tax_code\":\"" + taxCodes.get(0).getTaxCode() + "\"}]",
                "Response body content");
    }

    @Test(groups = "fast")
    public void getTaxCodesForTaxZoneProduct() throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("GET", "/taxCodes/NZ/memory-use");

        List<EasyTaxTaxCode> taxCodes = Arrays
                .asList(new EasyTaxTaxCode(UUID.randomUUID().toString()));
        given(dao.getTaxCodes(tenantId, "NZ", "memory-use", null, null)).willReturn(taxCodes);

        // when
        servlet.service(req, res);

        // then
        thenDefaultOkJsonResponse();

        assertEquals(byos.toString("UTF-8"),
                "[{\"tax_code\":\"" + taxCodes.get(0).getTaxCode() + "\"}]",
                "Response body content");
    }

    @Test(groups = "fast")
    public void getTaxCodesForTaxZoneProductTaxCode()
            throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("GET", "/taxCodes/NZ/memory-use/GST");

        List<EasyTaxTaxCode> taxCodes = Arrays
                .asList(new EasyTaxTaxCode(UUID.randomUUID().toString()));
        given(dao.getTaxCodes(tenantId, "NZ", "memory-use", "GST", null)).willReturn(taxCodes);

        // when
        servlet.service(req, res);

        // then
        thenDefaultOkJsonResponse();

        assertEquals(byos.toString("UTF-8"),
                "[{\"tax_code\":\"" + taxCodes.get(0).getTaxCode() + "\"}]",
                "Response body content");
    }

    @Test(groups = "fast")
    public void getTaxCodesValidNow() throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("GET", "/taxCodes");
        given(req.getParameter(EasyTaxServlet.VALID_NOW_PARAM)).willReturn("true");

        List<EasyTaxTaxCode> taxCodes = Arrays
                .asList(new EasyTaxTaxCode(UUID.randomUUID().toString()));
        given(dao.getTaxCodes(tenantId, null, null, null, now)).willReturn(taxCodes);

        // when
        servlet.service(req, res);

        // then
        thenDefaultOkJsonResponse();

        assertEquals(byos.toString("UTF-8"),
                "[{\"tax_code\":\"" + taxCodes.get(0).getTaxCode() + "\"}]",
                "Response body content");
    }

    @Test(groups = "fast")
    public void getTaxCodesValidAt() throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("GET", "/taxCodes");
        given(req.getParameter(EasyTaxServlet.VALID_DATE)).willReturn("2017-01-01T12:00:00Z");

        List<EasyTaxTaxCode> taxCodes = Arrays
                .asList(new EasyTaxTaxCode(UUID.randomUUID().toString()));
        DateTime at = new DateTime(2017, 1, 1, 12, 0, 0, DateTimeZone.UTC);
        given(dao.getTaxCodes(tenantId, null, null, null, at)).willReturn(taxCodes);

        // when
        servlet.service(req, res);

        // then
        thenDefaultOkJsonResponse();

        assertEquals(byos.toString("UTF-8"),
                "[{\"tax_code\":\"" + taxCodes.get(0).getTaxCode() + "\"}]",
                "Response body content");
    }

    @Test(groups = "fast")
    public void deleteAllTaxCodes() throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("DELETE", "/taxCodes");
        givenPermissions(TEST_USER, TEST_PASSWORD, TEST_PERMISSIONS);
        given(dao.removeTaxCodes(tenantId, null, null, null)).willReturn(1);

        // when
        servlet.service(req, res);

        // then
        thenAuthenticatedAs(TEST_USER, TEST_PASSWORD);
        then(dao).should().removeTaxCodes(tenantId, null, null, null);
        thenDefaultOkResponse();

        assertEquals(byos.toString("UTF-8"), "", "Response body content");
    }

    @Test(groups = "fast")
    public void deleteAllTaxCodesNotPermitted() throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("DELETE", "/taxCodes");
        givenPermissions(TEST_USER, TEST_PASSWORD, singleton(Permission.USER_CAN_VIEW));
        given(dao.removeTaxCodes(tenantId, null, null, null)).willReturn(1);

        // when
        servlet.service(req, res);

        // then
        thenDefaultResponse(403, null);

        assertEquals(byos.size(), 0, "Response body content");
    }

    @Test(groups = "fast")
    public void deleteAllTaxCodesNotAuthenticated()
            throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("DELETE", "/taxCodes");
        given(dao.removeTaxCodes(tenantId, null, null, null)).willReturn(1);

        // when
        servlet.service(req, res);

        // then
        thenDefaultResponse(401, null);

        assertEquals(byos.size(), 0, "Response body content");
    }

    @Test(groups = "fast")
    public void deleteAllTaxCodesTenantless() throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenTenantlessServletCall("DELETE", "/taxCodes");

        // when
        servlet.service(req, res);

        // then
        thenErrorResponse(404, "No tenant specified");

        assertEquals(byos.size(), 0, "Response body content");
    }

    @Test(groups = "fast")
    public void deleteTaxCodesForTaxZone() throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("DELETE", "/taxCodes/NZ");
        givenPermissions(TEST_USER, TEST_PASSWORD, TEST_PERMISSIONS);
        given(dao.removeTaxCodes(tenantId, "NZ", null, null)).willReturn(1);

        // when
        servlet.service(req, res);

        // then
        thenAuthenticatedAs(TEST_USER, TEST_PASSWORD);
        then(dao).should().removeTaxCodes(tenantId, "NZ", null, null);
        thenDefaultOkResponse();

        assertEquals(byos.toString("UTF-8"), "");
    }

    @Test(groups = "fast")
    public void deleteTaxCodesForTaxZoneProduct()
            throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("DELETE", "/taxCodes/NZ/memory-use");
        givenPermissions(TEST_USER, TEST_PASSWORD, TEST_PERMISSIONS);
        given(dao.removeTaxCodes(tenantId, "NZ", "memory-use", null)).willReturn(1);

        // when
        servlet.service(req, res);

        // then
        thenAuthenticatedAs(TEST_USER, TEST_PASSWORD);
        then(dao).should().removeTaxCodes(tenantId, "NZ", "memory-use", null);
        thenDefaultOkResponse();

        assertEquals(byos.toString("UTF-8"), "");
    }

    @Test(groups = "fast")
    public void deleteTaxCodesForTaxZoneProductTaxCode()
            throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("DELETE",
                "/taxCodes/NZ/memory-use/GST");
        givenPermissions(TEST_USER, TEST_PASSWORD, TEST_PERMISSIONS);
        given(dao.removeTaxCodes(tenantId, "NZ", "memory-use", "GST")).willReturn(1);

        // when
        servlet.service(req, res);

        // then
        thenAuthenticatedAs(TEST_USER, TEST_PASSWORD);
        then(dao).should().removeTaxCodes(tenantId, "NZ", "memory-use", "GST");
        thenDefaultOkResponse();

        assertEquals(byos.size(), 0, "Response body content");
    }

    @Test(groups = "fast")
    public void addTaxCode() throws IOException, ServletException, SQLException {
        // given
        byte[] data = "{\"tax_rate\":\"0.15\",\"valid_from_date\":\"2017-01-01T12:00:00Z\"}"
                .getBytes("UTF-8");
        ByteArrayInputStream byis = new ByteArrayInputStream(data);
        ByteArrayOutputStream byos = givenDefaultServletCall("POST", "/taxCodes/NZ/memory-use/GST",
                byis, data.length, EasyTaxServlet.APPLICATION_JSON_UTF8);
        givenPermissions(TEST_USER, TEST_PASSWORD, TEST_PERMISSIONS);

        // when
        servlet.service(req, res);

        // then
        thenAuthenticatedAs(TEST_USER, TEST_PASSWORD);
        thenDefaultResponse(201, null);
        then(res).should().setHeader(HttpHeaders.LOCATION,
                "/plugins/killbill-easytax/taxCodes/NZ/memory-use/GST");

        ArgumentCaptor<EasyTaxTaxCode> taxCodeCaptor = ArgumentCaptor
                .forClass(EasyTaxTaxCode.class);
        then(dao).should().saveTaxCode(taxCodeCaptor.capture());

        assertEquals(byos.size(), 0, "Response body content");
        EasyTaxTaxCode saved = taxCodeCaptor.getValue();
        assertEquals(saved.getCreatedDate(), now);
        assertEquals(saved.getKbTenantId(), tenantId);
        assertEquals(saved.getTaxZone(), "NZ");
        assertEquals(saved.getProductName(), "memory-use");
        assertEquals(saved.getTaxCode(), "GST");
        assertDateTimeEquals(saved.getValidFromDate(),
                new DateTime(2017, 1, 1, 12, 0, 0, DateTimeZone.UTC), "Valid from date");
        assertNull(saved.getValidToDate(), "Valid to date");
    }

    @Test(groups = "fast")
    public void addTaxCodeNotPermitted() throws IOException, ServletException, SQLException {
        // given
        byte[] data = "{\"tax_rate\":\"0.15\",\"valid_from_date\":\"2017-01-01T12:00:00Z\"}"
                .getBytes("UTF-8");
        ByteArrayInputStream byis = new ByteArrayInputStream(data);
        ByteArrayOutputStream byos = givenDefaultServletCall("POST", "/taxCodes/NZ/memory-use/GST",
                byis, data.length, EasyTaxServlet.APPLICATION_JSON_UTF8);
        givenPermissions(TEST_USER, TEST_PASSWORD, singleton(Permission.USER_CAN_VIEW));

        // when
        servlet.service(req, res);

        // then
        thenAuthenticatedAs(TEST_USER, TEST_PASSWORD);
        thenDefaultResponse(403, null);

        assertEquals(byos.size(), 0, "Response body content");
    }

    @Test(groups = "fast")
    public void addTaxCodeNotAuthenticated() throws IOException, ServletException, SQLException {
        // given
        byte[] data = "{\"tax_rate\":\"0.15\",\"valid_from_date\":\"2017-01-01T12:00:00Z\"}"
                .getBytes("UTF-8");
        ByteArrayInputStream byis = new ByteArrayInputStream(data);
        ByteArrayOutputStream byos = givenDefaultServletCall("POST", "/taxCodes/NZ/memory-use/GST",
                byis, data.length, EasyTaxServlet.APPLICATION_JSON_UTF8);

        // when
        servlet.service(req, res);

        // then
        thenDefaultResponse(401, null);

        assertEquals(byos.size(), 0, "Response body content");
    }

    @Test(groups = "fast")
    public void addTaxCodeTenantless() throws IOException, ServletException, SQLException {
        // given
        byte[] data = "{\"tax_rate\":\"0.15\",\"valid_from_date\":\"2017-01-01T12:00:00Z\"}"
                .getBytes("UTF-8");
        ByteArrayInputStream byis = new ByteArrayInputStream(data);
        ByteArrayOutputStream byos = givenTenantlessServletCall("POST",
                "/taxCodes/NZ/memory-use/GST", byis, data.length,
                EasyTaxServlet.APPLICATION_JSON_UTF8);

        // when
        servlet.service(req, res);

        // then
        thenErrorResponse(404, "No tenant specified");

        assertEquals(byos.size(), 0, "Response body content");
    }

    @SuppressWarnings("unchecked")
    @Test(groups = "fast")
    public void addTaxCodes() throws IOException, ServletException, SQLException {
        // given
        byte[] data = Resources.toByteArray(Resources.getResource(getClass(), "tax-codes-01.json"));
        ByteArrayInputStream byis = new ByteArrayInputStream(data);
        ByteArrayOutputStream byos = givenDefaultServletCall("POST", "/taxCodes", byis, data.length,
                EasyTaxServlet.APPLICATION_JSON_UTF8);
        givenPermissions(TEST_USER, TEST_PASSWORD, TEST_PERMISSIONS);

        // when
        servlet.service(req, res);

        // then
        thenAuthenticatedAs(TEST_USER, TEST_PASSWORD);
        thenDefaultOkResponse();

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> taxCodesCaptor = ArgumentCaptor.forClass(List.class);
        then(dao).should().saveTaxCodes(taxCodesCaptor.capture());

        assertEquals(byos.size(), 0, "Response body content");
        List<EasyTaxTaxCode> savedList = taxCodesCaptor.getValue();
        assertEquals(savedList.size(), 2, "Saved count");

        EasyTaxTaxCode saved = savedList.get(0);
        assertEquals(saved.getCreatedDate(), now);
        assertEquals(saved.getKbTenantId(), tenantId);
        assertEquals(saved.getTaxZone(), "NZ");
        assertEquals(saved.getProductName(), "memory-use");
        assertEquals(saved.getTaxCode(), "GST");
        assertDateTimeEquals(saved.getValidFromDate(),
                new DateTime(2017, 1, 1, 12, 0, 0, DateTimeZone.UTC), "Valid from date");
        assertDateTimeEquals(saved.getValidToDate(),
                new DateTime(2017, 9, 1, 1, 2, 3, DateTimeZone.UTC), "Valid to date");

        saved = savedList.get(1);
        assertEquals(saved.getCreatedDate(), now);
        assertEquals(saved.getKbTenantId(), tenantId);
        assertEquals(saved.getTaxZone(), "NZ");
        assertEquals(saved.getProductName(), "memory-use");
        assertEquals(saved.getTaxCode(), "GST");
        assertDateTimeEquals(saved.getValidFromDate(),
                new DateTime(2017, 9, 1, 1, 2, 3, DateTimeZone.UTC), "Valid from date");
        assertNull(saved.getValidToDate(), "Valid to date");
    }

}
