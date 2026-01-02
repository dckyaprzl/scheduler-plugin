package org.joget.scheduler;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormValidator;
import org.joget.commons.util.LogUtil;
import org.jsoup.nodes.FormElement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class JobNameValidator extends FormValidator {
    @Override
    public boolean validate(Element element, FormData formData, String[] values) {
        String jobName = (values != null && values.length > 0) ? values[0] : "";
        String jobId = formData.getRequestParameter("id");

        /* Required validation */
        if (jobName == null || jobName.trim().isEmpty()) {
            setErrorMessage(formData, element, "Job Name is required.");
            return false;
        }

        /* Duplicate validation */
        if (isDuplicate(jobName, jobId)) {
            setErrorMessage(formData, element, "Job Name already exists.");
            return false;
        }

        return true;
    }

    private void setErrorMessage(FormData formData, Element element, String message) {
        formData.addFormError(element.getPropertyString("id"), message);
    }


    private boolean isDuplicate(String jobName, String jobId) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            DataSource ds = (DataSource) AppUtil.getApplicationContext()
                    .getBean("setupDataSource");
            con = ds.getConnection();

            String sql = "SELECT COUNT(*) FROM sch_job_def WHERE name = ?";
            if (jobId != null && !jobId.isEmpty()) {
                sql += " AND id <> ?"; // exclude current record
            }

            ps = con.prepareStatement(sql);
            ps.setString(1, jobName);
            if (jobId != null && !jobId.isEmpty()) {
                ps.setString(2, jobId);
            }

            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Error validating job name");
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }

        return false;
    }

    @Override
    public String getName() {
        return "Job Name Validator";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "untuk validasi field job name";
    }

    @Override
    public String getLabel() {
        return "Job Name Validator";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return null;
    }
}
