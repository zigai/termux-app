package com.termux.app.terminal.keyboard.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import org.junit.Test;

import java.io.File;
import java.util.Set;

public class KeyboardSchemaTest {
    private static final File PUBLISHED =
            new File("../docs/keyboard/termux-keyboard.schema.json");
    private static final File PACKAGED =
            new File("src/main/assets/keyboard/termux-keyboard.schema.json");
    private static final File BUNDLED =
            new File("src/main/assets/keyboard/default-keyboard.json");

    @Test
    public void bundledFixtureConformsToPublishedAndPackagedSchemas() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode document = mapper.readTree(BUNDLED);

        assertEquals(mapper.readTree(PUBLISHED), mapper.readTree(PACKAGED));
        assertValid(mapper, document, PUBLISHED);
        assertValid(mapper, document, PACKAGED);
    }

    @Test
    public void schemasRejectMissingUnknownOutOfRangeAndUnsupportedValues() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode valid = (ObjectNode) mapper.readTree(BUNDLED);

        ObjectNode missingProfiles = valid.deepCopy();
        missingProfiles.remove("profiles");
        assertInvalidForBoth(mapper, missingProfiles);

        ObjectNode unknownField = valid.deepCopy();
        unknownField.put("unexpected", true);
        assertInvalidForBoth(mapper, unknownField);

        ObjectNode wideRails = valid.deepCopy();
        ((ObjectNode)
                        wideRails.at(
                                "/profiles/default/variants/0/landscapeSplit"))
                .put("railWidthPercent", 21);
        assertInvalidForBoth(mapper, wideRails);

        ObjectNode unsupportedAction = valid.deepCopy();
        ((ObjectNode)
                        unsupportedAction.at(
                                "/profiles/default/variants/0/rows/0/keys/0/action"))
                .put("type", "shellScript");
        assertInvalidForBoth(mapper, unsupportedAction);
    }

    private static void assertValid(ObjectMapper mapper, JsonNode document, File file)
            throws Exception {
        Set<ValidationMessage> errors = schema(mapper, file).validate(document);
        assertTrue(errors.toString(), errors.isEmpty());
    }

    private static void assertInvalidForBoth(ObjectMapper mapper, JsonNode document)
            throws Exception {
        assertInvalid(mapper, document, PUBLISHED);
        assertInvalid(mapper, document, PACKAGED);
    }

    private static void assertInvalid(ObjectMapper mapper, JsonNode document, File file)
            throws Exception {
        Set<ValidationMessage> errors = schema(mapper, file).validate(document);
        assertFalse(file + " accepted " + document, errors.isEmpty());
    }

    private static JsonSchema schema(ObjectMapper mapper, File file) throws Exception {
        JsonSchema schema =
                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                        .getSchema(mapper.readTree(file));
        return schema;
    }
}
