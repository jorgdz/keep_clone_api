
package ec.com.jnegocios.api.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import ec.com.jnegocios.exception.ErrorResponse;
import ec.com.jnegocios.exception.global.auth.AccountServiceException;
import ec.com.jnegocios.service.jwt.JWTService;
import ec.com.jnegocios.util.AppHelper;
import ec.com.jnegocios.util.JSONResponse;

public class JWTAuthenticationFilter 
	extends UsernamePasswordAuthenticationFilter {

	private AuthenticationManager authManager;
	private JWTService jwtManager;

	public JWTAuthenticationFilter(AuthenticationManager authManager, 
		ApplicationContext context) {

		this.authManager = authManager;
		this.jwtManager = context.getBean(JWTService.class);
		
		this.setRequiresAuthenticationRequestMatcher( 
			new AntPathRequestMatcher(AppHelper.PREFIX_ACC.concat("/login"), "POST") );

	}

	@SuppressWarnings("unchecked")
	@Override
	public Authentication attemptAuthentication(HttpServletRequest req, 
		HttpServletResponse res) throws AuthenticationException {
		
		String 
			username = obtainUsername(req),
			password = obtainPassword(req);
		UsernamePasswordAuthenticationToken userSigIn = null;

		if( (username == null) || (password == null) ) {

			Map<String, String> userData;

			try {
			
				userData = new ObjectMapper()
					.readValue(req.getInputStream(), HashMap.class);
				
				username = userData.get("username");
				password = userData.get("password");
				
				username = username != null ? username.trim().toLowerCase() : "";
				password = password != null ? password.trim().toLowerCase() : "";

			} catch (Exception e) {
		
				logger.error("Bad auth data format");
				username = "";
				password = "";
		
			}

		}

		userSigIn = new UsernamePasswordAuthenticationToken(username, password);

		return authManager.authenticate(userSigIn);

	}

	@Override
	protected void successfulAuthentication(HttpServletRequest req, 
		HttpServletResponse res, FilterChain chain, Authentication authResult) 
		throws IOException, ServletException {

		JSONResponse jsonResponse = JSONResponse.fromGeneralTemplate(
			req.getRequestURI(), 
			"Successful authentication, you have a new token", 
			200
		);

		res.addHeader(
			"Authorization", 
			"Bearer " + jwtManager.generateAuthToken((User) authResult.getPrincipal())
		);
		
		res.getWriter().write( 
			new ObjectMapper().writeValueAsString(jsonResponse.getBody()) );
		res.setStatus(200);
		res.setContentType("application/json");
	
	}

	@Override
	protected void unsuccessfulAuthentication(HttpServletRequest req, 
		HttpServletResponse res, AuthenticationException failed) 
		throws IOException, ServletException {

		ErrorResponse error = new ErrorResponse("Bad credentials", 
			new AccountServiceException(), req.getRequestURI(), 401);

        res.getWriter().write( new ObjectMapper().writeValueAsString(error) );
        res.setStatus(401);
        res.setContentType("application/json");
		
	}
	
}
