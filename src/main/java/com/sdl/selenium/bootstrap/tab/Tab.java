package com.sdl.selenium.bootstrap.tab;

import com.sdl.selenium.conditions.ConditionManager;
import com.sdl.selenium.conditions.RenderSuccessCondition;
import com.sdl.selenium.web.SearchType;
import com.sdl.selenium.web.WebLocator;
import com.sdl.selenium.web.XPathBuilder;
import com.sdl.selenium.web.link.WebLink;
import com.sdl.selenium.web.tab.ITab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tab extends WebLocator implements ITab {
    private static final Logger LOGGER = LoggerFactory.getLogger(Tab.class);

    private Tab() {
        withClassName("Tab");
        withBaseCls("nav nav-tabs");
    }

    public Tab(String title) {
        this();
        withText(title, SearchType.EQUALS);
    }

    public Tab(WebLocator container, String text) {
        this(text);
        withContainer(container);
    }

    private String getTitlePath(boolean active) {
        WebLink link = new WebLink().withText(getPathBuilder().getText(), SearchType.EQUALS);
        String isActive = active ? "@class='active'" : "not(@class='active')";
        WebLocator el = new WebLocator().withTag("ul").withClasses(getPathBuilder().getBaseCls());
        return el.getXPath() + "//li[" + isActive + "]" + link.getXPath();
    }

    /**
     * this method return the path of the main Tab (that contains also this Tab/Panel)
     *
     * @return the path of the main TabPanel
     */
    private String getBaseTabPanelPath() {
        String selector = getPathBuilder().getBasePath();
        WebLink link = new WebLink().withText(getPathBuilder().getText(), SearchType.EQUALS);
        selector += (selector.length() > 0 ? " and " : "") + "count(.//li[@class='active']" + link.getXPath() + ") > 0";
        return "//ul[" + selector + "]";
    }

    protected XPathBuilder createXPathBuilder() {
        return new XPathBuilder() {
            /**
             * this method return the path of only one visible div from the main TabPanel
             * @param disabled disabled
             * @return the path of only one visible div from the main TabPanel
             */
            @Override
            public String getItemPath(boolean disabled) {
                return getBaseTabPanelPath() + "//following-sibling::*[@class='tab-content']//*[@class='tab-pane active']";
            }
        };
    }

    /**
     * After the tab is set to active
     *
     * @return true or false
     */
    @Override
    public boolean setActive() {
        boolean activated = isActive();
        if (!activated) {
            WebLocator locator1 = new WebLocator(getPathBuilder().getContainer()).withElxPath(getTitlePath(false));
            WebLocator titleElement = locator1.withInfoMessage(getPathBuilder().getText() + " Tab");
            activated = titleElement.click();
        }
        if (activated) {
            LOGGER.info("setActive : " + toString());
        }
        return activated;
    }

    @Override
    public boolean isActive() {
        WebLocator locator = new WebLocator(getPathBuilder().getContainer()).withElxPath(getTitlePath(true));
        return new ConditionManager(200).add(new RenderSuccessCondition(locator)).execute().isSuccess();
    }
}
