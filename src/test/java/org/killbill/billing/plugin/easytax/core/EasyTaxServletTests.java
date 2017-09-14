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
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import org.killbill.billing.tenant.api.Tenant;
import org.killbill.clock.Clock;
import org.mockito.ArgumentCaptor;
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

    private EasyTaxDao dao;
    private Clock clock;
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
        servlet = new EasyTaxServlet(dao, clock);
        req = mock(HttpServletRequest.class);
        res = mock(HttpServletResponse.class);
    }

    private ByteArrayOutputStream givenDefaultServletCall(String method, String path)
            throws IOException {
        return givenDefaultServletCall(method, path, null, 0, null);
    }

    private ByteArrayOutputStream givenDefaultServletCall(String method, String path,
            InputStream content, int contentLength, String contentType) throws IOException {
        given(tenant.getId()).willReturn(tenantId);
        given(clock.getUTCNow()).willReturn(now);
        given(req.getAttribute("killbill_tenant")).willReturn(tenant);
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

        given(dao.removeTaxCodes(tenantId, null, null, null)).willReturn(1);

        // when
        servlet.service(req, res);

        // then
        thenDefaultOkResponse();

        assertEquals(byos.toString("UTF-8"), "", "Response body content");
    }

    @Test(groups = "fast")
    public void deleteTaxCodesForTaxZone() throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("DELETE", "/taxCodes/NZ");

        given(dao.removeTaxCodes(tenantId, "NZ", null, null)).willReturn(1);

        // when
        servlet.service(req, res);

        // then
        thenDefaultOkResponse();

        assertEquals(byos.toString("UTF-8"), "");
    }

    @Test(groups = "fast")
    public void deleteTaxCodesForTaxZoneProduct()
            throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("DELETE", "/taxCodes/NZ/memory-use");

        given(dao.removeTaxCodes(tenantId, "NZ", "memory-use", null)).willReturn(1);

        // when
        servlet.service(req, res);

        // then
        thenDefaultOkResponse();

        assertEquals(byos.toString("UTF-8"), "");
    }

    @Test(groups = "fast")
    public void deleteTaxCodesForTaxZoneProductTaxCode()
            throws IOException, ServletException, SQLException {
        // given
        ByteArrayOutputStream byos = givenDefaultServletCall("DELETE",
                "/taxCodes/NZ/memory-use/GST");

        given(dao.removeTaxCodes(tenantId, "NZ", "memory-use", "GST")).willReturn(1);

        // when
        servlet.service(req, res);

        // then
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

        // when
        servlet.service(req, res);

        // then
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

    @SuppressWarnings("unchecked")
    @Test(groups = "fast")
    public void addTaxCodes() throws IOException, ServletException, SQLException {
        // given
        byte[] data = Resources.toByteArray(Resources.getResource(getClass(), "tax-codes-01.json"));
        ByteArrayInputStream byis = new ByteArrayInputStream(data);

        ByteArrayOutputStream byos = givenDefaultServletCall("POST", "/taxCodes", byis, data.length,
                EasyTaxServlet.APPLICATION_JSON_UTF8);

        // when
        servlet.service(req, res);

        // then
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
