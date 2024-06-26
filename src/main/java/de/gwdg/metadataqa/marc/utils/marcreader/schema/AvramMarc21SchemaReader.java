package de.gwdg.metadataqa.marc.utils.marcreader.schema;

import de.gwdg.metadataqa.marc.EncodedValue;
import de.gwdg.metadataqa.marc.definition.Cardinality;
import de.gwdg.metadataqa.marc.definition.structure.ControlfieldPositionDefinition;
import de.gwdg.metadataqa.marc.definition.structure.DataFieldDefinition;
import de.gwdg.metadataqa.marc.definition.structure.DefaultControlFieldDefinition;
import de.gwdg.metadataqa.marc.definition.structure.Indicator;
import de.gwdg.metadataqa.marc.definition.structure.Marc21DataFieldDefinition;
import de.gwdg.metadataqa.marc.definition.structure.SubfieldDefinition;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;

public class AvramMarc21SchemaReader {
  private static final Logger logger = Logger.getLogger(AvramMarc21SchemaReader.class.getCanonicalName());
  private static final Map<String, Integer> knownFieldProperties = Map.of(
    "label", 1, "repeatable", 1, "indicator1", 1, "indicator2", 1, "subfields", 1, "positions", 1);
  private static final Map<String, Integer> knownSubfieldProperties = Map.of(
    "label", 1, "repeatable", 1, "codes", 1);
  private static final Map<String, Integer> knownSubfieldCodeProperties = Map.of(
    "label", 1, "code",1);
  private static final Map<String, Integer> knownIndicatorProperties = Map.of(
    "label", 1, "codes",1);
  private final JSONParser parser = new JSONParser(JSONParser.MODE_RFC4627);
  private final Map<String, DataFieldDefinition> map = new HashMap<>();

  public AvramMarc21SchemaReader(String fileName) {
    try {
      readFile(fileName);
    } catch (IOException | ParseException e) {
      logger.severe(e.getLocalizedMessage());
    }
  }

  public AvramMarc21SchemaReader(InputStream inputStream) {
    try {
      readStream(inputStream);
    } catch (ParseException e) {
      logger.severe(e.getLocalizedMessage());
    }
  }

  private void readFile(String fileName) throws IOException, ParseException {
    process((JSONObject) parser.parse(new FileReader(fileName)));
  }

  private void readStream(InputStream inputStream) throws ParseException {
    process((JSONObject) parser.parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8)));
  }

  private void process(JSONObject jsonObject) {
    JSONObject fields = (JSONObject) jsonObject.get("fields");
    for (Map.Entry<String, Object> entry : fields.entrySet()) {
      String id = entry.getKey();
      JSONObject field = (JSONObject) entry.getValue();
      processField(id, field);
    }
  }

  private void processField(String id, JSONObject field) {
    DataFieldDefinition tag = new Marc21DataFieldDefinition(
      id,
      (String) field.get("label"),
      (boolean) field.get("repeatable"),
      !field.containsKey("subfields")
    );
    Marc21DataFieldDefinition tagDef = (Marc21DataFieldDefinition)tag;

    if (field.containsKey("indicator1") && field.get("indicator1") != null)
      tagDef.setInd1(processIndicator(field.get("indicator1")));
    if (field.containsKey("indicator2") && field.get("indicator2") != null)
      tagDef.setInd2(processIndicator(field.get("indicator2")));
    if (field.containsKey("subfields"))
      processSubfields((JSONObject) field.get("subfields"), (Marc21DataFieldDefinition) tag);
    if (field.containsKey("positions"))
      processPositions((JSONObject) field.get("positions"), (Marc21DataFieldDefinition) tag);

    for (String property : field.keySet()) {
      if (!knownFieldProperties.containsKey(property)) {
        logger.warning(() -> "unhandled field property: " + property);
      }
    }
    if (id.equals("008") || id.equals("LDR")) {
      tag = new DefaultControlFieldDefinition((Marc21DataFieldDefinition) tag);
      if (id.equals("LDR")) {
        id = "leader";
      }
    }

    if (!map.containsKey(id)) {
      map.put(id, tag);
    } else {
      String warningMessage = "Duplicated field:" + id;
      logger.warning(warningMessage);
    }
  }

  private void processPositions(JSONObject positions, Marc21DataFieldDefinition tag) {
    TreeSet<ControlfieldPositionDefinition> sortedPositions = new TreeSet<>((a,b) -> a.getPositionStart() - b.getPositionStart());

    for (Map.Entry<String, Object> entry : positions.entrySet()) {
      String key = entry.getKey();
      sortedPositions.add(processPosition(key, positions.get(key)));
    }

    tag.setPositions(new LinkedList<>(sortedPositions));
  }

  private ControlfieldPositionDefinition processPosition(String positionKey, Object positionNode) {
    if (!(positionNode instanceof JSONObject)) {
      logger.warning(() -> "the positions node's type is not JSONObject, but " + positionNode.getClass().getCanonicalName());
      return null;
    }

    JSONObject position = (JSONObject) positionNode;
    String label = position.getAsString("label");

    String[] charPos = positionKey.split("-");
    int start = Integer.parseInt(charPos[0]);
    int end = charPos.length > 1 ? Integer.parseInt(charPos[1]) : start;
    if (position.containsKey("start")) {
      int startKey = (int) position.getAsNumber("start");
      if (startKey != start)
        logger.warning(() -> "position start " + start + "does not match character positions: " + positionKey);
    }
    if (position.containsKey("end")) {
      int endKey = (int) position.getAsNumber("end");
      if (endKey != end) {
        String warningMessage = "position end " + end + "does not match character positions: " + positionKey;
        logger.warning(warningMessage);
      }
    }
    end++;

    ControlfieldPositionDefinition definition = new ControlfieldPositionDefinition(label, start, end);
    if (!position.containsKey("codes")) {
      return definition;
    }

    definition.setCodes(extractEncodedValues(position));
    int length = end - start;
    if (length > 1
        && !definition.getCodes().isEmpty()
        && !definition.getCodes().get(0).getCode().equals("[number]")
        && definition.getCodes().get(0).getCode().length() < length) {
      definition.setRepeatableContent(true);
      definition.setUnitLength(definition.getCodes().get(0).getCode().length());
    }
    return definition;
  }

  private Indicator processIndicator(Object indicatorNode) {
    if (indicatorNode instanceof JSONObject) {
      JSONObject indicator = (JSONObject) indicatorNode;
      Indicator definition = new Indicator((String)indicator.get("label"));
      if (indicator.containsKey("codes")) {
        definition.setCodes(extractEncodedValues(indicator));
      }
      for (String key : indicator.keySet())
        if (!knownIndicatorProperties.containsKey(key))
          logger.warning(() -> "unhandled indicator property: " + key);

      return definition;
    } else {
      logger.warning(() -> "the indicator node's type is not JSONObject, but " + indicatorNode.getClass().getCanonicalName());
    }
    return null;
  }

  private static List<EncodedValue> extractEncodedValues(JSONObject valueContainer) {
    List<EncodedValue> codes = new LinkedList<>();
    for (Map.Entry<String, Object> value : ((JSONObject) valueContainer.get("codes")).entrySet()) {
      String code = value.getKey().equals("#") ? " " : value.getKey();
      codes.add(new EncodedValue(code, (String) value.getValue()));
    }
    return codes;
  }

  private void processSubfields(JSONObject subfields, Marc21DataFieldDefinition tag) {
    List<SubfieldDefinition> subfieldDefinitions = new LinkedList<>();
    if (subfields != null) {
      for (Map.Entry<String, Object> entry : subfields.entrySet()) {
        processSubfield(entry.getKey(), entry.getValue(), subfieldDefinitions);
      }
    }
    tag.setSubfields(subfieldDefinitions);
  }

  private void processSubfield(String code, Object o, List<SubfieldDefinition> subfieldDefinitions) {
    SubfieldDefinition definition = extractSubfield(code, o);
    if (definition != null)
      subfieldDefinitions.add(definition);
  }

  private SubfieldDefinition extractSubfield(String code, Object o) {
    SubfieldDefinition definition = null;
    if (o instanceof JSONObject) {
      JSONObject subfield = (JSONObject) o;
      String label = (String) subfield.get("label");
      boolean repeatable = (boolean) subfield.get("repeatable");
      String cardinalityCode = repeatable ? Cardinality.Repeatable.getCode() : Cardinality.Nonrepeatable.getCode();
      definition = new SubfieldDefinition(code, label, cardinalityCode);
      if (subfield.containsKey("codes"))
        definition.setCodes(processStaticValues(subfield.get("codes")));
      for (String key : subfield.keySet()) {
        if (!knownSubfieldProperties.containsKey(key))
          logger.warning(() -> "unhandled subfield property: " + key);
      }
    } else {
      logger.warning(() -> "the JSON node's type is not JSONObject, but " + o.getClass().getCanonicalName());
    }
    return definition;
  }

  private List<EncodedValue> processStaticValues(Object staticValuesObject) {
    List<EncodedValue> list = new LinkedList<>();
    if (!(staticValuesObject instanceof JSONObject)) {
      logger.warning(() -> "the staticValues is not a JSONObject, but " + staticValuesObject.getClass().getCanonicalName());
      return list;
    }

    for (Map.Entry<String, Object> codeEntry : ((JSONObject)staticValuesObject).entrySet()) {
      String code = codeEntry.getKey();
      if (!(codeEntry.getValue() instanceof JSONObject)) {
        logger.warning(() -> "the property set of a staticValue is not a JSONObject, but " + codeEntry.getValue().getClass().getCanonicalName());
        continue;
      }

      JSONObject property = (JSONObject) codeEntry.getValue();
      if (!code.equals(property.get("code"))) {
        String warningMessage = String.format("code is different to code value: %s vs %s", code, property.get("coden"));
        logger.warning(warningMessage);
      }
      else {
        list.add(new EncodedValue(code, (String) property.get("label")));
      }
      for (String key : property.keySet()) {
        if (!knownSubfieldCodeProperties.containsKey(key)) {
          logger.warning(() -> "unhandled subfield code property: " + key);
        }
      }
    }

    return list;
  }

  public Map<String, DataFieldDefinition> getMap() {
    return map;
  }
}
