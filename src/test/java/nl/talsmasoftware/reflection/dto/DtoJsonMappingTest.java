/*
 * Copyright 2016-2020 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.talsmasoftware.reflection.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.math.BigDecimal;

import static nl.talsmasoftware.reflection.Fixtures.fixture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author Sjoerd Talsma
 */
public class DtoJsonMappingTest {

    ObjectMapper mapper;
    DtoRepresentationV1 dto42;

    @BeforeEach
    public void setup() {
        mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        dto42 = new DtoRepresentationV1();
        dto42.number = 42L;
        dto42.name = "The Answer to the Ultimate Question of Life, the Universe, and Everything";
        dto42.amountInEuros = new BigDecimal("42.00");
        dto42.subObject = new DtoRepresentationV1();
        dto42.subObject.number = 13L;
        dto42.subObject.name = "Bad luck";
        dto42.subObject.amountInEuros = new BigDecimal("13.13");
    }

    @Test
    public void testJsonSerialization() throws IOException, JSONException {
        String json = mapper.writeValueAsString(dto42);
        JSONAssert.assertEquals(fixture("/fixtures/dto42.json"), json, true);
    }

    @Test
    public void testJsonDeserialization() throws IOException, JSONException {
        assertThat(mapper.readValue(fixture("/fixtures/dto42.json"), DtoRepresentationV1.class), is(equalTo(dto42)));
    }
}
