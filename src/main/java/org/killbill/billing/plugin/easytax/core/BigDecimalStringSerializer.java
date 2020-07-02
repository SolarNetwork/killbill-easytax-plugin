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

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Specialized JsonSerializer of {@link BigDecimal} to string values.
 * 
 * @author matt
 */
public class BigDecimalStringSerializer extends StdSerializer<BigDecimal> {

    /**
     * Singleton instance to use.
     */
    public static final BigDecimalStringSerializer INSTANCE = new BigDecimalStringSerializer();

    /**
     * Default constructor.
     * 
     * <p>
     * Note: usually you should NOT create new instances, but instead use {@link #INSTANCE} which is
     * stateless and fully thread-safe.
     * </p>
     */
    public BigDecimalStringSerializer() {
        super(BigDecimal.class);
    }

    /**
     * Construct with specific class.
     * 
     * @param handledType
     *            the type to use
     */
    public BigDecimalStringSerializer(Class<? extends BigDecimal> handledType) {
        super(handledType, false);
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeString(value.toPlainString());
    }

    @Override
    public void serializeWithType(BigDecimal value, JsonGenerator gen, SerializerProvider provider,
            TypeSerializer typeSer) throws IOException {
        typeSer.writeTypePrefixForScalar(value, gen);
        serialize(value, gen, provider);
        typeSer.writeTypeSuffixForScalar(value, gen);
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
            throws JsonMappingException {
        return createSchemaNode("string", true);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
            throws JsonMappingException {
        if (visitor != null) {
            visitor.expectStringFormat(typeHint);
        }
    }
}
