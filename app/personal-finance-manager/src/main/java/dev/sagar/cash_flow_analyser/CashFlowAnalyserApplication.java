package dev.sagar.cash_flow_analyser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;

@Theme(value = "my-app")
@Push
@SpringBootApplication
public class CashFlowAnalyserApplication implements AppShellConfigurator {

	public static void main(String[] args) {
		SpringApplication.run(CashFlowAnalyserApplication.class, args);
	}

}
