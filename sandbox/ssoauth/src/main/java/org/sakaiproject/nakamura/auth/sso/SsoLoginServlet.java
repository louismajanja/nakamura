/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.auth.sso;

import static org.sakaiproject.nakamura.api.auth.sso.SsoAuthConstants.SSO_LOGIN_PATH;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Helper servlet for SSO authentication. The servlet simply redirects to
 * the configured SSO server via AuthenticationHandler requestCredentials.
 * To avoid a loop, if the request is already authenticated, the servlet redirects to
 * the path specified by the request parameter "resource", or to the root
 * path.
 * <p>
 * Once all authentication modules use Sling's authtype approach to trigger
 * requestCredentials, it should also be possible to reach SSO through any servlet
 * (including sling.commons.auth's LoginServlet) by setting the
 * sling:authRequestLogin request parameter to "SSO".
 */
@SlingServlet(paths = { SSO_LOGIN_PATH }, methods = { "GET", "POST" })
public class SsoLoginServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = -1894135945816269913L;
  private static final Logger LOGGER = LoggerFactory.getLogger(SsoLoginServlet.class);

  @Reference
  protected transient SsoAuthenticationHandler ssoAuthnHandler;

  public SsoLoginServlet() {
  }

  protected SsoLoginServlet(SsoAuthenticationHandler ssoAuthHandler) {
    this.ssoAuthnHandler = ssoAuthHandler;
  }

  @Override
  protected void service(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    // Check for possible loop after authentication.
    if (request.getAuthType() != null) {
      String redirectTarget = ssoAuthnHandler.getReturnPath(request);
      if ((redirectTarget == null) || request.getRequestURI().equals(redirectTarget)) {
        redirectTarget = request.getContextPath() + "/";
      }
      LOGGER.info("Request already authenticated, redirecting to {}", redirectTarget);
      response.sendRedirect(redirectTarget);
      return;
    }

    // Pass control to the handler.
    if (!ssoAuthnHandler.requestCredentials(request, response)) {
      LOGGER.error("Unable to request credentials from handler");
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Cannot login");
    }
  }
}