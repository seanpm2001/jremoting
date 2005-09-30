/* ====================================================================
 * Copyright 2005 JRemoting Committers
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
package org.codehaus.jremoting.server;

import org.apache.commons.attributes.Attributes;

import java.lang.reflect.Method;

public class AttributeHelper {

    protected boolean isMethodAsync(Method method) {
        return Attributes.hasAttribute(method, "codehaus-remoting:method:async");
    }

    protected boolean isMethodAsyncCommit(Method method) {
        return Attributes.hasAttribute(method, "codehaus-remoting:method:commit");
    }

    protected boolean isMethodAsyncRollback(Method method) {
        return Attributes.hasAttribute(method, "codehaus-remoting:method:rollback");
    }

}
