package eu.nimble.catalogue.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.catalogue.impl.CatalogueController;
import eu.nimble.service.catalogue.impl.CatalogueLineController;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.config.CatalogueServiceConfig;
import eu.nimble.utility.config.PersistenceConfig;
import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.*;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {PersistenceConfig.class, CatalogueServiceConfig.class})
public class CatalogueLineControllerTest {
    private CatalogueLineController catalogueLineController = new CatalogueLineController();
    private static String UBLcatalogueUUID;

    // settings for h2
    private static String driver = "org.h2.Driver";
    private static String dialect = "org.hibernate.dialect.H2Dialect";
    private static String password = "";
    private static String username = "sa";
    //private static String url = "jdbc:h2:file:${user.home}/nimble/ubl;AUTO_SERVER=TRUE;MVCC=TRUE;AUTO_RECONNECT=TRUE;DB_CLOSE_DELAY=10;INIT=create schema IF NOT EXISTS APPS";
    private static String url = "jdbc:h2:mem:db2;MVCC=TRUE;AUTO_RECONNECT=TRUE;DB_CLOSE_DELAY=-1;INIT=create schema IF NOT EXISTS APPS";

    @BeforeClass
    public static void startH2DB() throws Exception{
        HibernateUtility.startH2DB();

        Map map = new HashMap();
        map.put("hibernate.connection.driver_class",driver);
        map.put("hibernate.dialect",dialect);
        map.put("hibernate.connection.password",password);
        map.put("hibernate.connection.username",username);
        map.put("hibernate.connection.url",url);
        map.put("hibernate.enable_lazy_load_no_trans","true");
        HibernateUtility.getInstance("ubl-data-model",map);

        // Create a catalogue
        CatalogueController catalogueController = new CatalogueController();
        String catalogueXML = IOUtils.toString(CatalogueControllerTest.class.getResourceAsStream("/UBL-CatalogueExample.xml"));
        HttpServletRequest httpServletRequest = new HttpServletRequest() {
            @Override
            public String getAuthType() {
                return null;
            }

            @Override
            public Cookie[] getCookies() {
                return new Cookie[0];
            }

            @Override
            public long getDateHeader(String s) {
                return 0;
            }

            @Override
            public String getHeader(String s) {
                return null;
            }

            @Override
            public Enumeration<String> getHeaders(String s) {
                return null;
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                return null;
            }

            @Override
            public int getIntHeader(String s) {
                return 0;
            }

            @Override
            public String getMethod() {
                return null;
            }

            @Override
            public String getPathInfo() {
                return null;
            }

            @Override
            public String getPathTranslated() {
                return null;
            }

            @Override
            public String getContextPath() {
                return null;
            }

            @Override
            public String getQueryString() {
                return null;
            }

            @Override
            public String getRemoteUser() {
                return null;
            }

            @Override
            public boolean isUserInRole(String s) {
                return false;
            }

            @Override
            public Principal getUserPrincipal() {
                return null;
            }

            @Override
            public String getRequestedSessionId() {
                return null;
            }

            @Override
            public String getRequestURI() {
                return null;
            }

            @Override
            public StringBuffer getRequestURL() {
                return new StringBuffer("UBL");
            }

            @Override
            public String getServletPath() {
                return null;
            }

            @Override
            public HttpSession getSession(boolean b) {
                return null;
            }

            @Override
            public HttpSession getSession() {
                return null;
            }

            @Override
            public String changeSessionId() {
                return null;
            }

            @Override
            public boolean isRequestedSessionIdValid() {
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromCookie() {
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromURL() {
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromUrl() {
                return false;
            }

            @Override
            public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
                return false;
            }

            @Override
            public void login(String s, String s1) throws ServletException {

            }

            @Override
            public void logout() throws ServletException {

            }

            @Override
            public Collection<Part> getParts() throws IOException, ServletException {
                return null;
            }

            @Override
            public Part getPart(String s) throws IOException, ServletException {
                return null;
            }

            @Override
            public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
                return null;
            }

            @Override
            public Object getAttribute(String s) {
                return null;
            }

            @Override
            public Enumeration<String> getAttributeNames() {
                return null;
            }

            @Override
            public String getCharacterEncoding() {
                return null;
            }

            @Override
            public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

            }

            @Override
            public int getContentLength() {
                return 0;
            }

            @Override
            public long getContentLengthLong() {
                return 0;
            }

            @Override
            public String getContentType() {
                return MediaType.APPLICATION_XML_VALUE;
            }

            @Override
            public ServletInputStream getInputStream() throws IOException {
                return null;
            }

            @Override
            public String getParameter(String s) {
                return null;
            }

            @Override
            public Enumeration<String> getParameterNames() {
                return null;
            }

            @Override
            public String[] getParameterValues(String s) {
                return new String[0];
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return null;
            }

            @Override
            public String getProtocol() {
                return null;
            }

            @Override
            public String getScheme() {
                return null;
            }

            @Override
            public String getServerName() {
                return null;
            }

            @Override
            public int getServerPort() {
                return 0;
            }

            @Override
            public BufferedReader getReader() throws IOException {
                return null;
            }

            @Override
            public String getRemoteAddr() {
                return null;
            }

            @Override
            public String getRemoteHost() {
                return null;
            }

            @Override
            public void setAttribute(String s, Object o) {

            }

            @Override
            public void removeAttribute(String s) {

            }

            @Override
            public Locale getLocale() {
                return null;
            }

            @Override
            public Enumeration<Locale> getLocales() {
                return null;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public RequestDispatcher getRequestDispatcher(String s) {
                return null;
            }

            @Override
            public String getRealPath(String s) {
                return null;
            }

            @Override
            public int getRemotePort() {
                return 0;
            }

            @Override
            public String getLocalName() {
                return null;
            }

            @Override
            public String getLocalAddr() {
                return null;
            }

            @Override
            public int getLocalPort() {
                return 0;
            }

            @Override
            public ServletContext getServletContext() {
                return null;
            }

            @Override
            public AsyncContext startAsync() throws IllegalStateException {
                return null;
            }

            @Override
            public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
                return null;
            }

            @Override
            public boolean isAsyncStarted() {
                return false;
            }

            @Override
            public boolean isAsyncSupported() {
                return false;
            }

            @Override
            public AsyncContext getAsyncContext() {
                return null;
            }

            @Override
            public DispatcherType getDispatcherType() {
                return null;
            }
        };

        // Standard = UBL
        ResponseEntity responseEntity = catalogueController.addXMLCatalogue("UBL",catalogueXML,httpServletRequest);
        CatalogueLineControllerTest.UBLcatalogueUUID = ((CatalogueType)responseEntity.getBody()).getUUID();
    }

    @AfterClass
    public static void stopH2DB() {
        // Delete catalogue
        // Standard = UBL
        CatalogueController catalogueController = new CatalogueController();
        catalogueController.deleteCatalogue("UBL",CatalogueLineControllerTest.UBLcatalogueUUID);

        HibernateUtility.stopH2DB();
    }

    @Test
    public void test1_addCatalogueLine() throws Exception {
        catalogueLineController.getCatalogueLine(CatalogueLineControllerTest.UBLcatalogueUUID,"TestX");

        String catalogueJSON = IOUtils.toString(CatalogueControllerTest.class.getResourceAsStream("/CatalogueLineExample.txt"));
        ResponseEntity responseEntity = catalogueLineController.addCatalogueLine(CatalogueLineControllerTest.UBLcatalogueUUID,catalogueJSON);

        int statusCode = responseEntity.getStatusCodeValue();

        boolean check = false;
        if(statusCode == 200 || statusCode == 201)
            check = true;

        Assert.assertTrue(check);
    }

    @Test
    public void test2_getCatalogueLine() throws Exception{
        ResponseEntity<CatalogueLineType> responseEntity = catalogueLineController.getCatalogueLine(CatalogueLineControllerTest.UBLcatalogueUUID,"TestX");

        Assert.assertEquals("NotUpdated",responseEntity.getBody().getOrderableUnit());
    }

    @Test
    public void test3_updateCatalogueLine() throws Exception{
        ObjectMapper objectMapper = new ObjectMapper();
        ResponseEntity<CatalogueLineType> responseEntity = catalogueLineController.getCatalogueLine(CatalogueLineControllerTest.UBLcatalogueUUID,"TestX");
        CatalogueLineType catalogueLineType = responseEntity.getBody();

        catalogueLineType.setOrderableUnit("updated");

        String catalogueLineTypeAsString = objectMapper.writeValueAsString(catalogueLineType);

        ResponseEntity responseEntity1 = catalogueLineController.updateCatalogueLine(CatalogueLineControllerTest.UBLcatalogueUUID,catalogueLineTypeAsString);

        Assert.assertEquals("updated",((CatalogueLineType)responseEntity1.getBody()).getOrderableUnit());
    }

    @Test
    public void test4_deleteCatalogueLineById() throws Exception {
        ResponseEntity responseEntity = catalogueLineController.deleteCatalogueLineById(CatalogueLineControllerTest.UBLcatalogueUUID,"TestX");
        Assert.assertEquals(200,responseEntity.getStatusCodeValue());
    }

}
