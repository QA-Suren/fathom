/*
 * Copyright (C) 2015 the original author or authors.
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
 */

package fathom.authc;

import com.google.common.base.Preconditions;

/**
 * Represents standard username/password credentials.
 *
 * @author James Moger
 */
public class StandardCredentials implements AuthenticationToken, Credentials {

    private final String username;
    private final String password;

    public StandardCredentials(String username, String password) {
        Preconditions.checkNotNull(username, "Username may not be null!");
        this.username = username.trim();
        this.password = password == null ? null : password.trim();
    }

    @Override
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }


    @Override
    public Credentials sanitize() {
        return new StandardCredentials(username, null);
    }

    @Override
    public String toString() {
        return "StandardCredentials{" +
                "username='" + username + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StandardCredentials that = (StandardCredentials) o;

        if (password != null ? !password.equals(that.password) : that.password != null) return false;
        if (username != null ? !username.equals(that.username) : that.username != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        return result;
    }

}
