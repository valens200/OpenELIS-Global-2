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
* Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
*
*/
package org.openelisglobal.reports.action.implementation.reportBeans;

//import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.SAMPLE_STATUS;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.ANALYSIS_STATUS;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DATE_TIME;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DICT_RAW;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.LOG;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.NONE;

//import org.openelisglobal.common.services.StatusService;

//import org.apache.commons.validator.GenericValidator;

//import org.openelisglobal.common.services.TestService;
//import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.reports.form.ReportForm.DateType;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class StudyVLColumnBuilder extends CIStudyColumnBuilder {
    private DateType dateType;

    public StudyVLColumnBuilder(DateRange dateRange, String projectStr) {
        super(dateRange, projectStr);
    }

    public StudyVLColumnBuilder(DateRange dateRange, String projectStr, DateType dateType) {
        super(dateRange, projectStr);
        this.dateType = dateType;
    }

    // @Override
    @Override
    protected void defineAllReportColumns() {
        defineBasicColumns();
        add("Viral Load", "VIRAL LOAD", NONE);
        add("Viral Load", "VIRAL LOAD LOG", LOG);
        add("type_of_sample_name", "TYPE_OF_SAMPLE", NONE);
        add("analysis_status_id", "ANALYSIS_STATUS", ANALYSIS_STATUS);
        add("started_date", "STARTED_DATE", DATE_TIME);
        add("completed_date", "COMPLETED_DATE", DATE_TIME);
        add("released_date", "RELEASED_DATE", DATE_TIME);

        add("hivStatus", "STATVIH", DICT_RAW);
        add("nameOfDoctor", "NAMEMED", NONE);
        add("nameOfSampler", "NAMEPRELEV", NONE);
        add("arvTreatmentInitDate", "ARV_INIT_DATE", NONE);
        add("arvTreatmentRegime", "ARVREG");

        add("currentARVTreatmentINNs1", "CURRENT1", NONE);
        add("currentARVTreatmentINNs2", "CURRENT2", NONE);
        add("currentARVTreatmentINNs3", "CURRENT3", NONE);
        add("currentARVTreatmentINNs4", "CURRENT4", NONE);

        add("currentARVTreatment", "CURRENT_ART");
        add("vlReasonForRequest", "VL_REASON", DICT_RAW);
        add("vlOtherReasonForRequest", "REASON_OTHER", NONE);

        add("initcd4Count", "INITCD4_COUNT", NONE);
        add("initcd4Percent", "INITCD4_PERCENT", NONE);
        add("initcd4Date", "INITCD4_DATE", NONE);

        add("demandcd4Count", "DEMANDCD4_COUNT", NONE);
        add("demandcd4Percent", "DEMANDCD4_PERCENT", NONE);
        add("demandcd4Date", "DEMANDCD4_DATE", NONE);

        add("vlBenefit", "PRIOR_VL_BENEFIT");
        add("vlPregnancy", "VL_PREGNANCY");
        add("vlSuckle", "VL_SUCKLE");
        add("priorVLLab", "PRIOR_VL_Lab", NONE);
        add("priorVLValue", "PRIOR_VL_Value", NONE);
        add("priorVLDate", "PRIOR_VL_Date", NONE);

        add("report_name", "REPORT_NAME", NONE);
        add("report_generation_time", "PRINTED_DATE", DATE_TIME);
        add("report_lastupdated", "LAST_REPORT_UPDATE", DATE_TIME);

        // addAllResultsColumns();

    }

    /**
     * @return the SQL for (nearly) one big row for each sample in the date range
     *         for the particular project.
     */

    @Override
    public void makeSQL() {
        // Switch date column according to selected DateType: PK
        String dateColumn = "s.entered_date ";
        switch (dateType) {
        case ORDER_DATE:
            dateColumn = "s.entered_date ";
            break;
        case RESULT_DATE:
            dateColumn = "a.released_date ";
            break;
        case PRINT_DATE:
            dateColumn = "dt.report_generation_time ";
        default:
            break;
        }
        // String validStatusId =
        // StatusService.getInstance().getStatusID(StatusService.AnalysisStatus.Finalized);
        // String validStatusId2 =
        // StatusService.getInstance().getStatusID(StatusService.AnalysisStatus.Finalized);

        Test test = SpringContext.getBean(TestService.class).getActiveTestByName("Viral Load").get(0);
        query = new StringBuilder();
        String lowDatePostgres = postgresDateFormat.format(dateRange.getLowDate());
        String highDatePostgres = postgresDateFormat.format(dateRange.getHighDate());
        query.append(SELECT_SAMPLE_PATIENT_ORGANIZATION);
        // all crosstab generated tables need to be listed in the following list and in
        // the WHERE clause at the bottom
        query.append(
                "\n, a.started_date,a.completed_date,a.released_date,a.printed_date, a.status_id as analysis_status_id, r.value as \"Viral Load\",a.type_of_sample_name, demo.*, currentARVTreatmentINNs.*, dt.name as report_name, first_dt.report_generation_time, dt.lastupdated as report_lastupdated ");

        query.append(FROM_SAMPLE_PATIENT_ORGANIZATION);

        // --------------------------
        // all observation history values
        appendObservationHistoryCrosstab(dateRange.getLowDate(), dateRange.getHighDate());
        // current ARV treatments
        appendRepeatingObservation(SQLConstant.CURRENT_ARV_TREATMENT_INNS, 4, dateRange.getLowDate(),
                dateRange.getHighDate());
        // result
        // appendResultCrosstab(dateRange.getLowDate(), dateRange.getHighDate() );
        query.append(",  clinlims.analysis as a \n");
        // -------------------------------------

        query.append(" LEFT JOIN  clinlims.result as r on r.analysis_id = a.id \n"
                + " LEFT JOIN  clinlims.sample_item as si on si.id = a.sampitem_id \n"
                + " LEFT JOIN  clinlims.sample as s on s.id = si.samp_id \n"
                + " LEFT JOIN  (select max(id)as id, row_id  from clinlims.document_track \n"
                + "           group by (row_id )  order by row_id DESC) as dtr on dtr.row_id=s.id \n"
                + " LEFT JOIN clinlims.document_track as dt on dtr.id=dt.id \n"
		        + " LEFT JOIN  (select min(id)as id, row_id from clinlims.document_track \n"
		        + " group by (row_id ) order by row_id ASC) as first_dtr on first_dtr.row_id=s.id \n"
		        + " LEFT JOIN clinlims.document_track as first_dt on first_dtr.id=first_dt.id \n");

        // and finally the join that puts these all together. Each cross table should be
        // listed here otherwise it's not in the result and you'll get a full join
        query.append(" WHERE " + "\n a.test_id =" + test.getId() + "\n AND a.sampitem_id = si.id"
                + "\n AND s.id = si.samp_id" + "\n AND s.id=sh.samp_id" + "\n AND sh.patient_id=pat.id"
                + "\n AND pat.person_id = per.id" + "\n AND s.id=so.samp_id" + "\n AND so.org_id=o.id"
                + "\n AND s.id = sp.samp_id" + "\n AND s.id=demo.s_id" + "\n AND s.id = currentARVTreatmentINNs.samp_id"
                + "\n AND " + dateColumn + " >= date('" + lowDatePostgres + "')" + "\n AND " + dateColumn + " <= date('"
                + highDatePostgres + "')"

//--------------
                + "\n ORDER BY s.accession_number;");
        /////////
        // no don't insert another crosstab or table here, go up before the main WHERE
        // clause
        return;
    }

}
