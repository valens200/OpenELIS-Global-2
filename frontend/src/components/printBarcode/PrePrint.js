import { React, useState, useEffect } from "react";
import { FormattedMessage, useIntl, injectIntl } from "react-intl";
import {
  Checkbox,
  Select,
  SelectItem,
  Grid,
  Column,
  TextInput,
  NumberInput,
  Button,
} from "@carbon/react";
import { getFromOpenElisServer } from "../utils/Utils";
import { sampleTypeTestsStructure } from "../data/SampleEntryTestsForTypeProvider";

const PrePrint = () => {
  const intl = useIntl();

  const [sampleTypes, setSampleTypes] = useState([]);
  const [selectedSampleTypeId, setSelectedSampleTypeId] = useState(null);

  const [sampleTypeTests, setSampleTypeTests] = useState(
    sampleTypeTestsStructure
  );
  const [selectedTests, setSelectedTests] = useState([]);
  const [selectedPanels, setSelectedPanels] = useState([]);

  const [labelSets, setLabelSets] = useState(1);
  const [orderLabelsPerSet, setOrderLabelsPerSet] = useState(1);
  const [specimenLabelsPerSet, setSpecimenLabelsPerSet] = useState(1);
  const [facilityId, setFacilityId] = useState("");

  const [source, setSource] = useState("about:blank");
  const [renderBarcode, setRenderBarcode] = useState(false);

  function findTestById(testId) {
    return sampleTypeTests.tests.find((test) => test.id === testId);
  }
  function findTestIndex(testId) {
    return sampleTypeTests.tests.findIndex((test) => test.id === testId);
  }

  function updateSampleTypeTests(test, userBenchChoice = false) {
    let tests = [...sampleTypeTests.tests];
    let testIndex = findTestIndex(test.id);
    tests[testIndex].userBenchChoice = userBenchChoice;
    setSampleTypeTests({ ...sampleTypeTests, tests: tests });
  }

  const triggerPanelCheckBoxChange = (isChecked, testMaps) => {
    const testIds = testMaps.split(",").map((id) => id.trim());
    testIds.map((testId) => {
      let testIndex = findTestIndex(testId);
      let test = findTestById(testId);
      if (testIndex !== -1) {
        updateSampleTypeTests(test, isChecked);
        if (isChecked) {
          setSelectedTests((prevState) => {
            return [...prevState, { id: test.id, name: test.name }];
          });
        } else {
          removeTestFromSelectedTests(test);
        }
      }
    });
  };

  const addPanelToSelectedPanels = (panel) => {
    setSelectedPanels([
      ...selectedPanels,
      { id: panel.panelId, name: panel.name, testMaps: panel.testMaps },
    ]);
    triggerPanelCheckBoxChange(true, panel.testMaps);
  };

  const removePanelFromSelectedPanels = (panel) => {
    let index = 0;
    let panelId = panel.id !== undefined ? panel.id : panel.panelId;

    for (let i in selectedPanels) {
      if (selectedPanels[i].id === panelId) {
        triggerPanelCheckBoxChange(false, selectedPanels[i].testMaps);
        const newPanels = selectedPanels;
        newPanels.splice(index, 1);
        setSelectedPanels([...newPanels]);
        break;
      }
      index++;
    }
  };

  const removeTestFromSelectedTests = (test) => {
    let index = 0;
    for (let i in selectedTests) {
      if (selectedTests[i].id === test.id) {
        const newTests = selectedTests;
        newTests.splice(index, 1);
        setSelectedTests([...newTests]);
        break;
      }
      index++;
    }
  };

  function addTestToSelectedTests(test) {
    setSelectedTests([...selectedTests, { id: test.id, name: test.name }]);
  }
  const handlePanelCheckbox = (e, panel) => {
    if (e.currentTarget.checked) {
      addPanelToSelectedPanels(panel);
    } else {
      removePanelFromSelectedPanels(panel);
    }
  };
  const handleTestCheckbox = (e, test) => {
    if (e.currentTarget.checked) {
      addTestToSelectedTests(test);
    } else {
      removeTestFromSelectedTests(test);
    }
  };
  const fetchSamplesTypes = (res) => {
    setSampleTypes(res);
  };

  const fetchSampleTypeTests = (res) => {
    setSampleTypeTests(res);
  };

  const handleFetchSampleTypeTests = (e) => {
    const { value } = e.target;
    setSelectedSampleTypeId(value);
    setSelectedTests([]);
    setSelectedPanels([]);
  };

  const prePrintLabels = () => {
    console.log(selectedPanels);
    const selectedTestIds = selectedTests
      .map((selectedTest) => selectedTest.id)
      .join(",");
    const params = new URLSearchParams({
      prePrinting: "true",
      numSetsOfLabels: labelSets,
      numOrderLabelsPerSet: orderLabelsPerSet,
      numSpecimenLabelsPerSet: specimenLabelsPerSet,
      facilityName: facilityId,
      testIds: selectedTestIds,
    });
    setSource(`LabelMakerServlet?${params.toString()}`);
    setRenderBarcode(true);
  };

  useEffect(() => {
    getFromOpenElisServer("/rest/user-sample-types", fetchSamplesTypes);
  }, []);

  useEffect(() => {
    if (selectedSampleTypeId !== null) {
      getFromOpenElisServer(
        `/rest/sample-type-tests?sampleType=${selectedSampleTypeId}`,
        fetchSampleTypeTests
      );
    }
  }, [selectedSampleTypeId]);

  return (
    <>
      <div className="orderLegendBody">
        <Grid>
          <Column lg={16}>
            <h4>
              <FormattedMessage id="barcode.print.preprint" />
            </h4>
          </Column>
          <Column lg={16}>
            <NumberInput
              min={1}
              max={100}
              defaultValue={1}
              onChange={(_, state) => setLabelSets(state.value)}
              label={"Number of label sets"}
              id="labelSets"
              className="inputText"
            />
          </Column>
          <Column lg={8}>
            <NumberInput
              min={1}
              max={100}
              defaultValue={1}
              onChange={(_, state) => setOrderLabelsPerSet(state.value)}
              label={"Number of order labels per set"}
              id="orderLabelsPerSet"
              className="inputText"
            />
          </Column>
          <Column lg={8}>
            <NumberInput
              min={1}
              max={100}
              defaultValue={1}
              onChange={(_, state) => setSpecimenLabelsPerSet(state.value)}
              label={"Number of specimen labels per set"}
              id="specimenLabelsPerSet"
              className="inputText"
            />
          </Column>
          <Column lg={8}>
            <NumberInput
              readOnly
              value={labelSets * (orderLabelsPerSet + specimenLabelsPerSet)}
              label={"Total Labels to Print"}
              id="totalLabelsToPrint"
              className="inputText"
            />
          </Column>
          <Column lg={8}>
            <TextInput
              id="facilityId"
              onChange={(e) => setFacilityId(e.target.value)}
              labelText={"Facility ID"}
            />
          </Column>
        </Grid>
      </div>
      <div className="orderLegendBody">
        <Grid>
          <Column lg={16}>
            <h4>
              <FormattedMessage id="label.button.sample" />
            </h4>
          </Column>
          <Column lg={16}>
            <Select
              id="selectSampleType"
              className="selectSampleType"
              labelText="Sample Type"
              onChange={(e) => {
                handleFetchSampleTypeTests(e);
              }}
            >
              {selectedSampleTypeId === null && (
                <SelectItem
                  text={intl.formatMessage({ id: "sample.select.type" })}
                  value=""
                />
              )}
              {sampleTypes?.map((sampleType, i) => (
                <SelectItem
                  text={sampleType.value}
                  value={sampleType.id}
                  key={i}
                />
              ))}
            </Select>
          </Column>
          <Column lg={4}>
            {selectedSampleTypeId && (
              <h4>
                <FormattedMessage id="sample.entry.panels" />
              </h4>
            )}
            {sampleTypeTests.panels !== null &&
              sampleTypeTests.panels.map((panel) => {
                return panel.name === "" ? (
                  ""
                ) : (
                  <Checkbox
                    onChange={(e) => handlePanelCheckbox(e, panel)}
                    labelText={panel.name}
                    id={"panel_" + panel.panelId}
                    key={panel.panelId}
                    checked={
                      selectedPanels.filter((item) => item.id === panel.panelId)
                        .length > 0
                    }
                  />
                );
              })}
          </Column>
          <Column lg={4}>
            {selectedSampleTypeId && (
              <h4>
                <FormattedMessage id="sample.entry.available.tests" />
              </h4>
            )}
            {sampleTypeTests.tests != null &&
              sampleTypeTests.tests.map((test) => {
                return test.name === "" ? (
                  ""
                ) : (
                  <Checkbox
                    onChange={(e) => handleTestCheckbox(e, test)}
                    labelText={test.name}
                    id={"test_" + test.id}
                    key={test.id}
                    checked={
                      selectedTests.filter((item) => item.id === test.id)
                        .length > 0
                    }
                  />
                );
              })}
          </Column>
          <Column lg={16}>
            <FormattedMessage id="barcode.print.preprint.note" />
          </Column>
          <Column lg={16}>
            <Button disabled={!selectedSampleTypeId} onClick={prePrintLabels}>
              <FormattedMessage id="barcode.print.preprint.button" />
            </Button>
          </Column>
        </Grid>
      </div>
      {renderBarcode && (
        <div className="orderLegendBody">
          <Grid>
            <Column lg={16}>
              <h4>
                <FormattedMessage id="barcode.header" />
              </h4>
            </Column>
          </Grid>
          <iframe src={source} width="100%" height="500px" />
        </div>
      )}
    </>
  );
};
export default injectIntl(PrePrint);
