package dev.sagar.cash_flow_analyser.views;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route("about")
@Menu(title = "About", order = 4)
@Uses(Icon.class)
public class AboutView extends VerticalLayout {

  public AboutView() {
    setAlignItems(Alignment.CENTER);
    setJustifyContentMode(JustifyContentMode.CENTER);

    H2 title = new H2("About FinVision AI");
    title.getStyle().set("margin-bottom", "2rem");

    Image image = new Image("images/app.png", "Finance & Budget Analysis");
    image.setHeight(300, Unit.PIXELS);
    image.setWidth(500, Unit.PIXELS);

    H3 tagline = new H3("Your Financial Future, In Focus");
    tagline.getStyle().set("margin-top", "2rem");
    tagline.getStyle().set("color", "var(--lumo-primary-color)");

    Paragraph description = new Paragraph(
        """
            FinVision AI is a comprehensive ecosystem of applications designed to help users manage their finances through intelligent automation
            and AI-powered assistance.
            This project combines multiple specialized applications to provide features like transaction categorization, budget analysis,
            financial health tracking, and batch processing of financial data.
            """);
    description.getStyle().set("max-width", "800px").set("text-align", "center").set("margin-top",
        "1rem");

    add(title, image, tagline, description);
  }
}
