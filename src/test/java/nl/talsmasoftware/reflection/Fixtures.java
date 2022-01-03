/*
 * Copyright 2016-2022 Talsma ICT
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
package nl.talsmasoftware.reflection;

import nl.talsmasoftware.reflection.dto.DtoJsonMappingTest;

import java.io.*;
import java.util.logging.Logger;

/**
 * @author Sjoerd Talsma
 */
public class Fixtures {

    public static String fixture(final String name) {
        InputStream in = null;
        try {
            // Stacktrace elements 0: getStacktrace(), 1: fixture(), 2: caller!
            in = Class.forName(Thread.currentThread().getStackTrace()[2].getClassName()).getResourceAsStream(name);
            if (in == null) throw new IllegalArgumentException(String.format("Fixture not found: \"%s\".", name));
            Reader reader = new InputStreamReader(in, "UTF-8");
            StringWriter writer = new StringWriter();
            char[] buf = new char[1024];
            for (int read = reader.read(buf); read >= 0; read = reader.read(buf)) {
                writer.write(buf, 0, read);
            }
            return writer.toString();
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalStateException(String.format("Caller class for fixture \"%s\" not found: %s",
                    name, cnfe.getMessage()), cnfe);
        } catch (IOException ioe) {
            throw new IllegalStateException(String.format("Exception reading fixture \"%s\": %s",
                    name, ioe.getMessage()), ioe);
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException ioe) {
                Logger.getLogger(DtoJsonMappingTest.class.getName()).finest("Error closing input stream: " + ioe);
            }
        }
    }

}
