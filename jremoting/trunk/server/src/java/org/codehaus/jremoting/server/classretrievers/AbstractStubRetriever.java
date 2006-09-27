/* ====================================================================
 * Copyright 2005-2006 JRemoting Committers
 * Portions copyright 2001 - 2004 Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.codehaus.jremoting.server.classretrievers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.codehaus.jremoting.server.StubRetrievalException;
import org.codehaus.jremoting.server.StubRetriever;
import org.codehaus.jremoting.util.StubHelper;

/**
 * Class AbstractStubRetriever
 *
 * @author Paul Hammant
 */
public abstract class AbstractStubRetriever implements StubRetriever {

    private ClassLoader classLoader;

    /**
     * Set the classloader to use.
     *
     * @param classLoader the classloader to use.
     */
    protected void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Method getProxyClassBytes
     *
     * @param publishedName the publication name
     * @return a byte array for the Bean.
     * @throws StubRetrievalException if the bytes cannot be retrieved.
     */
    public final byte[] getStubClassBytes(String publishedName) throws StubRetrievalException {
        return getThingBytes(StubHelper.formatStubClassName(publishedName));
    }

    /**
     * Method getThingBytes
     *
     * @param thingName the publication name
     * @return a byte array for the thing.
     * @throws StubRetrievalException if the bytes cannot be retrieved.
     */
    protected byte[] getThingBytes(String thingName) throws StubRetrievalException {

        InputStream is = null;

        thingName = thingName.replace('.', '\\') + ".class";

        try {
            is = classLoader.getResourceAsStream(thingName);
        } catch (Exception e) {
            throw new StubRetrievalException("Generated class not found in classloader specified : " + e.getMessage());
        }

        if (is == null) {
            throw new StubRetrievalException("Generated class not found in classloader specified.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i = 0;

        try {
            while (-1 != (i = is.read())) {
                baos.write(i);
            }

            is.close();
        } catch (IOException e) {
            throw new StubRetrievalException("Error retrieving generated class bytes : " + e.getMessage());
        }

        return baos.toByteArray();
    }
}
