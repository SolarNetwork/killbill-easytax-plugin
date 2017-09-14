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

package org.killbill.billing.plugin.easytax.dao;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.jooq.Binding;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingGetSQLInputContext;
import org.jooq.BindingGetStatementContext;
import org.jooq.BindingRegisterContext;
import org.jooq.BindingSQLContext;
import org.jooq.BindingSetSQLOutputContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.impl.DSL;

/**
 * Converter so that all time stamp values are stored in the UTC time zone.
 * 
 * @author matt
 */
public class JodaDateTimeBinding implements Binding<Timestamp, DateTime> {

    /** The UTC TimeZone. */
    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static final long serialVersionUID = -9220532743945253219L;

    private static final class JodaDateTimeConverter implements Converter<Timestamp, DateTime> {

        private static final long serialVersionUID = -5878080033665957529L;

        @Override
        public DateTime from(Timestamp databaseObject) {
            return (databaseObject != null ? new DateTime(databaseObject.getTime()) : null);
        }

        @Override
        public Timestamp to(DateTime userObject) {
            return (userObject != null ? new Timestamp(userObject.getMillis()) : null);
        }

        @Override
        public Class<Timestamp> fromType() {
            return Timestamp.class;
        }

        @Override
        public Class<DateTime> toType() {
            return DateTime.class;
        }

    }

    private final Converter<Timestamp, DateTime> converter;

    public JodaDateTimeBinding() {
        this.converter = new JodaDateTimeConverter();
    }

    @Override
    public Converter<Timestamp, DateTime> converter() {
        return converter;
    }

    @Override
    public void sql(BindingSQLContext<DateTime> ctx) throws SQLException {
        ctx.render().visit(DSL.val(ctx.convert(converter()).value(), Timestamp.class));
    }

    @Override
    public void register(BindingRegisterContext<DateTime> ctx) throws SQLException {
        ctx.statement().registerOutParameter(ctx.index(), Types.TIMESTAMP);
    }

    @Override
    public void set(BindingSetStatementContext<DateTime> ctx) throws SQLException {
        ctx.statement().setTimestamp(ctx.index(), ctx.convert(converter()).value(),
                Calendar.getInstance(UTC));
    }

    @Override
    public void set(BindingSetSQLOutputContext<DateTime> ctx) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void get(BindingGetResultSetContext<DateTime> ctx) throws SQLException {
        ctx.convert(converter())
                .value(ctx.resultSet().getTimestamp(ctx.index(), Calendar.getInstance(UTC)));
    }

    @Override
    public void get(BindingGetStatementContext<DateTime> ctx) throws SQLException {
        ctx.convert(converter())
                .value(ctx.statement().getTimestamp(ctx.index(), Calendar.getInstance(UTC)));
    }

    @Override
    public void get(BindingGetSQLInputContext<DateTime> ctx) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

}
