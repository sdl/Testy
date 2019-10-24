package com.sdl.selenium.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.sdl.selenium.WebLocatorSuggestions;
import com.sdl.selenium.WebLocatorUtils;
import com.sdl.selenium.utils.config.WebDriverConfig;
import com.sdl.selenium.utils.config.WebLocatorConfig;
import com.sdl.selenium.web.utils.FileUtils;
import com.sdl.selenium.web.utils.MultiThreadClipboardUtils;
import com.sdl.selenium.web.utils.RetryUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class WebLocatorDriverExecutor implements WebLocatorExecutor {

    private WebDriver driver;

    public WebLocatorDriverExecutor(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public boolean click(WebLocator el) {
        boolean click = false;
        if (!el.getCurrentElementPath().equals(getSelector(el))) {
            isElementPresent(el);
            click = RetryUtils.retryRunnableSafe(1, () -> el.getWebElement().click());
        }
        if (!click) {
            click = RetryUtils.retryRunnableSafe(4, () -> {
                findAgain(el);
                el.getWebElement().click();
            });
        }
        return click;
    }

    @Override
    public boolean clickAt(WebLocator el) {
        focus(el);
        return click(el);
    }

    @Override
    public boolean doubleClickAt(WebLocator el) {
        boolean clicked = false;
        if (findAgain(el)) {
            try {
                Actions builder = new Actions(driver);
                builder.doubleClick(el.getWebElement()).perform();
                clicked = true;
            } catch (Exception e) {
                // http://code.google.com/p/selenium/issues/detail?id=244
                log.info("Exception in doubleClickAt {}", e);
                clicked = fireEventWithJS(el, "dblclick") != null;
            }
        }
        return clicked;
    }

    public boolean submit(WebLocator el) {
        boolean submit = false;
        if (!el.getCurrentElementPath().equals(getSelector(el))) {
            isElementPresent(el);
            submit = RetryUtils.retryRunnableSafe(1, () -> el.getWebElement().submit());
        }
        if (!submit) {
            submit = RetryUtils.retryRunnableSafe(4, () -> {
                findAgain(el);
                el.getWebElement().submit();
            });
        }
        return submit;
    }

    @Override
    public boolean clear(WebLocator el) {
        boolean clear = false;
        if (!el.getCurrentElementPath().equals(getSelector(el))) {
            isElementPresent(el);
            clear = RetryUtils.retryRunnableSafe(1, () -> el.getWebElement().clear());
        }
        if (!clear) {
            clear = RetryUtils.retryRunnableSafe(4, () -> {
                findAgain(el);
                el.getWebElement().clear();
            });
        }
        return clear;
    }

    @Override
    public boolean sendKeys(WebLocator el, java.lang.CharSequence... charSequences) {
        boolean sendKeys = false;
        if (ensureExists(el)) {
            try {
                el.currentElement.sendKeys(charSequences);
                sendKeys = true;
            } catch (ElementNotVisibleException e) {
                try {
                    sendKeys = tryAgainDoSendKeys(el, charSequences);
                } catch (ElementNotVisibleException ex) {
                    try {
                        mouseOver(el);
                        sendKeys = tryAgainDoSendKeys(el, charSequences);
                    } catch (ElementNotVisibleException exc) {
                        log.error("final ElementNotVisibleException in sendKeys: {}", el, exc);
                        throw exc;
                    }
                }
            } catch (WebDriverException e) {
                //TODO this fix is for Chrome
                Actions builder = new Actions(driver);
                builder.click(el.currentElement);
                builder.sendKeys(charSequences);
                sendKeys = true;
            }
        }
        return sendKeys;
    }

    private boolean tryAgainDoSendKeys(WebLocator el, java.lang.CharSequence... charSequences) {
        if (findAgain(el)) {
            el.currentElement.sendKeys(charSequences); // not sure it will click now
            return true;
        } else {
            log.error("currentElement is null after to try currentElement: {}", el);
            return false;
        }
    }

    @Override
    public boolean setValue(WebLocator el, String value) {
        return doSetValue(el, value);
    }

    private boolean doSetValue(WebLocator el, String value) {
        int lengthVal = WebLocatorConfig.getMinCharsToType();
        int length = value.length();
//        el.getWebElement().clear();
        el.getWebElement().sendKeys(Keys.CONTROL, "a");
        el.getWebElement().sendKeys(Keys.DELETE);
        if (lengthVal == -1 || length <= lengthVal) {
            el.currentElement.sendKeys(value);
            log.info("Set value({}): '{}'", el, getLogValue(el, value));
        } else {
            try {
                MultiThreadClipboardUtils.copyString(value);
            } catch (IllegalStateException e) {
                log.debug("IllegalStateException: cannot open system clipboard - try again.");
                MultiThreadClipboardUtils.copyString(value);
            }
            MultiThreadClipboardUtils.pasteString(el);
            el.currentElement.sendKeys(value.substring(length - 1));
            log.info("Paste value({}): string with size: '{}'", el, length);
        }
        return true;
    }

    private String getLogValue(WebLocator el, String value) {
        String info = el.getPathBuilder().getInfoMessage();
        if (Strings.isNullOrEmpty(info)) {
            info = el.getPathBuilder().itemToString();
        }
        info = info.toLowerCase();

        return WebLocatorConfig.getLogParamsExclude().contains(info) ? "*****" : value;
    }

    @Override
    public String getCssValue(final WebLocator el, final String propertyName) {
        String cssValue = null;
        if (!el.getCurrentElementPath().equals(getSelector(el))) {
            isElementPresent(el);
            cssValue = RetryUtils.retrySafe(1, () -> el.getWebElement().getCssValue(propertyName));
        }
        if (cssValue == null) {
            return RetryUtils.retrySafe(4, () -> {
                findAgain(el);
                return el.getWebElement().getCssValue(propertyName);
            });
        }
        return cssValue;
    }

    @Override
    public String getTagName(final WebLocator el) {
        String tagName = null;
        if (!el.getCurrentElementPath().equals(getSelector(el))) {
            isElementPresent(el);
            tagName = RetryUtils.retrySafe(1, () -> el.getWebElement().getTagName());
        }
        if (tagName == null) {
            return RetryUtils.retrySafe(4, () -> {
                findAgain(el);
                return el.getWebElement().getTagName();
            });
        }
        return tagName;
    }

    @Override
    public String getAttribute(final WebLocator el, final String attribute) {
        String attributeValue = null;
        if (!el.getCurrentElementPath().equals(getSelector(el))) {
            isElementPresent(el);
            attributeValue = RetryUtils.retrySafe(1, () -> el.getWebElement().getAttribute(attribute));
        }
        if (attributeValue == null) {
            return RetryUtils.retrySafe(4, () -> {
                findAgain(el);
                return el.getWebElement().getAttribute(attribute);
            });
        }
        return attributeValue;
    }

    public String getAttributeId(WebLocator el) {
        String id = getAttribute(el, "id");
        if (el.hasId()) {
            final String pathId = el.getPathBuilder().getId();
            if (!id.equals(pathId)) {
                log.warn("id is not same as pathId:{} - {}", id, pathId);
            }
            return id;
        }
        return id;
    }

    private boolean ensureExists(final WebLocator el) {
        boolean present = el.currentElement != null || isElementPresent(el);
        if (!present) {
            log.debug("Element not found: {}", el);
        }
        return present;
    }

    @Override
    public String getText(WebLocator el) {
        String text = null;
        if (!el.getCurrentElementPath().equals(getSelector(el))) {
            isElementPresent(el);
            text = RetryUtils.retrySafe(1, () -> el.getWebElement().getText());
        }
        if (text == null) {
            return RetryUtils.retrySafe(4, () -> {
                findAgain(el);
                return el.getWebElement().getText();
            });
        }
        return text;
    }

    private String getSelector(WebLocator el) {
        String css = el.getCssSelector();
        return Strings.isNullOrEmpty(css) ? el.getXPath() : css;
    }

    private boolean findAgain(WebLocator el) {
        invalidateCache(el);
        return isElementPresent(el);
    }

    private void invalidateCache(WebLocator el) {
        el.currentElement = null;
        el.setCurrentElementPath("");
    }

    @Override
    public String getValue(WebLocator el) {
        return getAttribute(el, "value");
    }

    @Override
    public boolean isElementPresent(WebLocator el) {
        findElement(el);
        return el.currentElement != null;
    }

    @Override
    public WebElement findElement(WebLocator el) {
        final String path = getSelector(el);
//        if (isSamePath(el, path)) {
//            log.debug("currentElement already found one time: " + el);
        //return el.currentElement;
//        }
        doWaitElement(el, Duration.ZERO);
        el.setCurrentElementPath(path);
        return el.currentElement;
    }

    public List<WebElement> findElements(WebLocator el) {
        return driver.findElements(el.getSelector());
    }

    @Override
    public WebElement waitElement(final WebLocator el, final long millis, boolean showXPathLog) {
        return waitElement(el, Duration.ofMillis(millis), showXPathLog);
    }

    @Override
    public WebElement waitElement(final WebLocator el, Duration duration, boolean showXPathLog) {
        doWaitElement(el, duration);
        if (el.currentElement == null && showXPathLog) {
            log.warn("Element not found after {} seconds; {}", duration.getSeconds(), el);
            logDetails(el);
        }
        return el.currentElement;
    }

    private void logDetails(WebLocator el) {
        if (WebLocatorConfig.isLogXPath()) {
            log.debug("\t" + WebLocatorUtils.getFirebugXPath(el));
        }
        if (WebLocatorConfig.isLogSuggestions()) {
            WebLocatorSuggestions.getSuggestion(el);
        }
    }

    private WebElement doWaitElement(final WebLocator el, Duration duration) {
        Wait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(duration)
                .pollingEvery(Duration.ofMillis(1))
                .ignoring(NoSuchElementException.class)
                .ignoring(ElementNotVisibleException.class)
                .ignoring(WebDriverException.class);

        try {
            if (el.getPathBuilder().isVisibility()) {
                el.currentElement = wait.until(ExpectedConditions.visibilityOfElementLocated(el.getSelector()));
            } else {
                el.currentElement = wait.until(d -> d.findElement(el.getSelector()));
            }
        } catch (TimeoutException e) {
            el.currentElement = null;
        }
        return el.currentElement;
    }

    @Override
    public int size(WebLocator el) {
        return findElements(el).size();
    }

    @Override
    public Point getLocation(WebLocator el) {
        return ensureExists(el) ? el.currentElement.getLocation() : null;
    }

    @Override
    public Dimension getSize(WebLocator el) {
        return ensureExists(el) ? el.currentElement.getSize() : null;
    }

    @Override
    public Rectangle getRect(WebLocator el) {
        return ensureExists(el) ? el.currentElement.getRect() : null;
    }

    @Override
    public boolean focus(WebLocator el) {
        return fireEventWithJS(el, "mouseover") != null;
    }

    @Override
    public boolean mouseOver(WebLocator el) {
        boolean mouseOver;
        try {
            if (findAgain(el)) {
                Actions builder = new Actions(driver);
                builder.moveToElement(el.getWebElement()).perform();
                mouseOver = true;
            } else {
                mouseOver = false;
            }
        } catch (WebDriverException e) {
            log.error("Could not mouse over {}, {}", el, e);
            mouseOver = false;
        }
        return mouseOver;
    }

    @Override
    public boolean blur(WebLocator el) {
        return fireEventWithJS(el, "blur") != null;
    }

    @Override
    public boolean isSelected(WebLocator el) {
        return findAgain(el) && el.currentElement.isSelected();
    }

    public boolean isDisplayed(WebLocator el) {
        return findAgain(el) && el.currentElement.isDisplayed();
    }

    public boolean isEnabled(WebLocator el) {
        return findAgain(el) && el.currentElement.isEnabled();
    }

    public boolean isSamePath(WebLocator el, String path) {
        return el.currentElement != null && (el.getCurrentElementPath().equals(path));
    }

    private boolean isCached(WebLocator el) {
        boolean cached = false; // TODO config
        return cached;
    }

    @Override
    public Object executeScript(String script, Object... objects) {
        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) driver;
        try {
            return javascriptExecutor.executeScript(script, objects);
        } catch (WebDriverException e) {
            log.error("WebDriverException in executeScript: " + script, e);
            return null;
        }
    }

    public Object fireEventWithJS(WebLocator el, String eventName) {
        String script = "if(document.createEvent){" +
                "var evObj = document.createEvent('MouseEvents');\n" +
                "evObj.initEvent('" + eventName + "', true, true);\n" +
                "return fireOnThis.dispatchEvent(evObj);\n" +
                "} else if(document.createEventObject) {" +
                "return fireOnThis.fireEvent('on" + eventName + "');" +
                "}";
        String id = getAttributeId(el);
        String cls;
        if (!Strings.isNullOrEmpty(id)) {
            script = "var fireOnThis = document.getElementById('" + id + "');\n" + script;
        } else if (!"".equals(cls = getAttribute(el, "class"))) {
            script = "var fireOnThis = document.getElementsByClassName('" + cls + "')[0];\n" + script;
        } else {
            script = "var fireOnThis = document.evaluate(\"" + el.getXPath() + "\", document, null, XPathResult.ANY_TYPE, null).iterateNext();\n" +
                    "var evObj = document.createEvent('MouseEvents');\n" +
                    "evObj.initEvent( '" + eventName + "', true, true );\n" +
                    "return fireOnThis.dispatchEvent(evObj);";
        }
        return executeScript(script);
    }

    @Override
    public void highlight(WebLocator el) {
        highlightElementWithDriver(el.currentElement);
    }

    public boolean download(String fileName, long timeoutMillis) {
        if (WebDriverConfig.isSilentDownload()) {
            if (WebDriverConfig.isHeadless() && WebDriverConfig.isChrome()) {
                Map<String, Object> commandParams = new HashMap<>();
                commandParams.put("cmd", "Page.setDownloadBehavior");
                Map<String, String> params = new HashMap<>();
                params.put("behavior", "allow");
                params.put("downloadPath", WebDriverConfig.getDownloadPath());
                commandParams.put("params", params);
                ObjectMapper objectMapper = new ObjectMapper();
                HttpClient httpClient = HttpClientBuilder.create().build();
                try {
                    String command = objectMapper.writeValueAsString(commandParams);
                    String uri = WebDriverConfig.getDriverService().getUrl().toString() + "/session/" + ((ChromeDriver) driver).getSessionId() + "/chromium/send_command";
                    HttpPost request = new HttpPost(uri);
                    request.addHeader("content-type", "application/json");
                    request.setEntity(new StringEntity(command));
                    httpClient.execute(request);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            fileName = WebDriverConfig.getDownloadPath() + File.separator + fileName;
            File file = new File(fileName);
            return FileUtils.waitFileIfIsEmpty(file, timeoutMillis) && fileName.equals(file.getAbsolutePath());
        } else {
            return RunExe.getInstance().download(fileName);
        }
    }

    public boolean browse(WebLocator el) {
        try {
            el.focus();
            Actions builder = new Actions(driver);
            builder.moveToElement(el.currentElement).perform();
            builder.click().perform();
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * @param filePath e.g. "C:\\text.txt"
     * @return true of false
     */
    public boolean upload(String filePath) {
        return RunExe.getInstance().upload(filePath);
    }

    private void highlightElementWithDriver(WebElement el) {

        // TODO more tests for this method

//        String highlightStyle = "background: none yellow !important; color: red !important; border: 1px solid green !important;";

//        String script = "(function(element){" +
//            "var original_style = element.getAttribute('style') || '';" +
//            "element.setAttribute('style', original_style + '; " + highlightStyle + "'); " +
//            "setTimeout(function(){element.setAttribute('style', original_style);}, 200);})(arguments[0]);";

//        executeScript(script, element);

//        for (int i = 0; i < 2; i++) {
//            JavascriptExecutor js = (JavascriptExecutor) driver;
//            js.executeScript("arguments[0].setAttribute('style', arguments[1]);", element, highlightStyle);
//            js.executeScript("arguments[0].setAttribute('style', arguments[1]);", element, "");
//        }
    }
}