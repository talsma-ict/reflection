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
package nl.talsmasoftware.reflection.strings;

public class NonStringCharSequence implements CharSequence {
    private final CharSequence delegate;

    public NonStringCharSequence(CharSequence delegate) {
        if (delegate == null) throw new NullPointerException("Delegate CharSequence is <null>");
        this.delegate = delegate;
    }

    public int length() {
        return delegate.length();
    }

    public char charAt(int index) {
        return delegate.charAt(index);
    }

    public CharSequence subSequence(int start, int end) {
        return delegate.subSequence(start, end);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
