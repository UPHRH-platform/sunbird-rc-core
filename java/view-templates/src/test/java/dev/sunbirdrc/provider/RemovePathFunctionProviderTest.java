package dev.sunbirdrc.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class RemovePathFunctionProviderTest {

	RemovePathFunctionProvider removePathFunctionProvider;

	@Before
	public void setUp() throws Exception {
		removePathFunctionProvider = new RemovePathFunctionProvider();
	}

	@Test
	public void shouldRemovePathsFromJsonNode() throws JsonProcessingException {
		JsonNode objectNode = new ObjectMapper().readTree("{\n" +
				"  \"details\": {\n" +
				"    \"name\": \"Test\",\n" +
				"    \"age\": 10\n" +
				"  }\n" +
				"}");
		List<Object> actualValues = new ArrayList<>();
		actualValues.add(objectNode.get("details"));

		JsonNode updatedNode = removePathFunctionProvider.doAction(actualValues, new String[]{"$.details", "$.age"});
		JsonNode expected = new ObjectMapper().readTree("{\n" +
				"  \"name\": \"Test\"\n" +
				"}");
		Assert.assertEquals(expected, updatedNode);
	}
}
