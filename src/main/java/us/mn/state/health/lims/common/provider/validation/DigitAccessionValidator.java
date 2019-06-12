/**
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package us.mn.state.health.lims.common.provider.validation;

import java.util.HashSet;
import java.util.Set;

import spring.mine.internationalization.MessageUtil;
import spring.service.sample.SampleService;
import spring.util.SpringContext;

public class DigitAccessionValidator implements IAccessionNumberValidator {
	
	protected SampleService sampleService = SpringContext.getBean(SampleService.class);

	private String incrementStartingValue = "0000001";
	private int upperIncRange = 9999999;
	private int maxLength = 7;
	private static final boolean NEED_PROGRAM_CODE = false;
	private static Set<String> REQUESTED_NUMBERS = new HashSet<>();
	private String format;

	public DigitAccessionValidator(int length) {
		format = "%0" + String.valueOf(length) + "d";
		incrementStartingValue = String.format(format, 1);
		String upper = incrementStartingValue.replace("0", "9").replace("1", "9");
		upperIncRange = Integer.parseInt(upper);
		maxLength = length;
	}

	@Override
	public boolean needProgramCode() {
		return NEED_PROGRAM_CODE;
	}

	@Override
	public String createFirstAccessionNumber(String programCode) {
		return incrementStartingValue;
	}

	@Override
	public String incrementAccessionNumber(String currentHighAccessionNumber) throws IllegalStateException {

		int increment = Integer.parseInt(currentHighAccessionNumber);

		if (increment < upperIncRange) {
			increment++;
		} else {
			throw new IllegalStateException("AccessionNumber has no next value");
		}

		return String.format(format, increment);
	}

	@Override
	public ValidationResults validFormat(String accessionNumber, boolean checkDate) {

		if (accessionNumber.length() != maxLength) {
			return ValidationResults.LENGTH_FAIL;
		}

		try {
			Integer.parseInt(accessionNumber);
		} catch (NumberFormatException e) {
			return ValidationResults.FORMAT_FAIL;
		}

		return ValidationResults.SUCCESS;
	}

	@Override
	public String getInvalidMessage(ValidationResults results) {
		switch (results) {
		case LENGTH_FAIL:
			return MessageUtil.getMessage("sample.entry.invalid.accession.number.length");
		case USED_FAIL:
			return MessageUtil.getMessage("sample.entry.invalid.accession.number.suggestion") + " "
					+ getNextAvailableAccessionNumber(null);
		case FORMAT_FAIL:
			return getInvalidFormatMessage(results);
		default:
			return MessageUtil.getMessage("sample.entry.invalid.accession.number");
		}
	}

	@Override
	public String getInvalidFormatMessage(ValidationResults results) {
		return MessageUtil.getMessage("sample.entry.invalid.accession.number.format.corrected",
				new String[] { getFormatPattern(), getFormatExample() });
	}

	private String getFormatPattern() {
		return "#######";
	}

	private String getFormatExample() {
		return "0000012";
	}

	@Override
	public String getNextAvailableAccessionNumber(String prefix) throws IllegalStateException {
		String nextAccessionNumber;
		String curLargestAccessionNumber = sampleService.getLargestAccessionNumber();

		if (curLargestAccessionNumber == null) {
			nextAccessionNumber = incrementStartingValue;
		} else {
			nextAccessionNumber = incrementAccessionNumber(curLargestAccessionNumber);

			while (REQUESTED_NUMBERS.contains(nextAccessionNumber)) {
				nextAccessionNumber = incrementAccessionNumber(nextAccessionNumber);
			}

			REQUESTED_NUMBERS.add(nextAccessionNumber);
		}

		return nextAccessionNumber;
	}

	@Override
	public int getMaxAccessionLength() {
		return maxLength;
	}

	// recordType parameter is not used in this case
	@Override
	public boolean accessionNumberIsUsed(String accessionNumber, String recordType) {
		return sampleService.getSampleByAccessionNumber(accessionNumber) != null;
	}

	@Override
	public ValidationResults checkAccessionNumberValidity(String accessionNumber, String recordType, String isRequired,
			String projectFormName) {

		ValidationResults results = validFormat(accessionNumber, false);
		// TODO refactor accessionNumberIsUsed into two methods so the null
		// isn't needed. (Its only used for program accession number)
		if (results == ValidationResults.SUCCESS && accessionNumberIsUsed(accessionNumber, null)) {
			results = ValidationResults.USED_FAIL;
		}

		return results;
	}

	@Override
	public int getInvarientLength() {
		return 0;
	}

	@Override
	public int getChangeableLength() {
		return maxLength;
	}

	@Override
	public String getPrefix() {
		return null; // no fixed prefix
	}
}
