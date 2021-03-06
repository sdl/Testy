package com.sdl.selenium.extjs3.button;

import com.sdl.selenium.extjs3.ExtJsComponent;
import com.sdl.selenium.utils.config.WebDriverConfig;
import com.sdl.selenium.web.WebLocator;
import com.sdl.selenium.web.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplitButton extends Button {
    private static final Logger LOGGER = LoggerFactory.getLogger(SplitButton.class);

    public SplitButton() {
        setClassName("SplitButton");
        setBaseCls("x-btn");
        setVisibility(true);
    }

    public SplitButton(ExtJsComponent container) {
        this();
        setContainer(container);
    }

    public SplitButton(ExtJsComponent container, String text) {
        this(container);
        setText(text);
    }

    public boolean clickOnMenu(String option) {
        return clickOnMenu(new String[]{option});
    }

    public boolean clickOnMenu(String[] menuOptions) {
        int n = menuOptions.length;
        LOGGER.debug("clickOnMenu : " + menuOptions[n - 1]);
        ready();
        String info = toString();
        if (!isEnabled()) {
            // waiting period for some buttons to become enabled (monitor valid)
            LOGGER.debug("Button is disabled. Waiting ...");
            Utils.sleep(1000);
        }
        if (isEnabled()) {
            LOGGER.info("Click on button " + info);
            // TODO try to use Menu class for implementing select item
//            click();
            showMenu();
            WebLocator menu = new WebLocator("x-menu-floating");
            if (WebDriverConfig.isIE()) {
                if (menu.ready()) {
                    LOGGER.info("In IE is visible");
                }
            } else {
                menu.setStyle("visibility: visible;");
            }
            menu.setInfoMessage("active menu");
            ExtJsComponent option = new ExtJsComponent(menu);
            for (String menuOption : menuOptions) {
                option.setText(menuOption);
                if (!option.mouseOver()) {
                    return false;
                }
            }
            if (option.clickAt()) {
                LOGGER.info("Button Select menu option : " + option.getPathBuilder().getText());
                return true;
            } else {
                LOGGER.warn("Could not locate option '" + option.getPathBuilder().getText() + "'. Performing simple click on button : " + info);
                clickAt();
            }
        } else {
            LOGGER.error("(" + info + ") doesn't exists or is disabled. " + getXPath());
        }
        return false;
    }
}
