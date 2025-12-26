package org.joget.scheduler;

import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBinder;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormLoadBinder;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.model.FormStoreBinder;
import org.joget.commons.util.UuidGenerator;
import org.joget.plugin.property.service.PropertyUtil;
import org.joget.scheduler.dao.JobDefinitionDao;
import org.joget.scheduler.model.JobDefinition;

public class SchedulerFormBinder extends FormBinder implements FormLoadBinder, FormStoreBinder {

    @Override
    public String getName() {
        return "SchedulerFormBinder";
    }

    @Override
    public String getVersion() {
        return "6.0.0";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getLabel() {
        return "SchedulerFormBinder";
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        FormRowSet results = new FormRowSet();
        if (primaryKey != null && primaryKey.trim().length() > 0) {
            JobDefinitionDao jobDefinitionDao = (JobDefinitionDao) AppContext.getInstance().getAppContext().getBean("jobDefinitionDao");
            JobDefinition jobDefinition = jobDefinitionDao.get(primaryKey);

            if (jobDefinition != null) {
                FormRow row = new FormRow();
                row.setId(jobDefinition.getId());
                row.setProperty("applicationId", jobDefinition.getAppId());
                row.setProperty("name", jobDefinition.getName());
                row.setProperty("frequencyType", jobDefinition.getFrequencyType());
                row.setProperty("hour", jobDefinition.getHour());
                row.setProperty("minute", jobDefinition.getMinute());
                row.setProperty("dateOfWeek", jobDefinition.getDateOfWeek());
                row.setProperty("dayOfMonth", jobDefinition.getDayOfMonth());
                row.setProperty("subject", jobDefinition.getSubject());
                row.setProperty("pluginClass", jobDefinition.getPluginClass());
                row.setProperty("pluginProperties", PropertyUtil.propertiesJsonLoadProcessing(jobDefinition.getPluginProperties()));
                row.setProperty("trigger", jobDefinition.getTrigger());

                results.add(row);
            }
        }
        return results;
    }

    @Override
    public FormRowSet store(Element element, FormRowSet rows, FormData formData) {
        if (rows != null && !rows.isEmpty()) {
            JobDefinitionDao jobDefinitionDao = (JobDefinitionDao) AppContext.getInstance().getAppContext().getBean("jobDefinitionDao");
            FormRow row = rows.get(0);
            JobDefinition jobDefinition = null;

            if (row.getProperty("pluginProperties") == null || row.getProperty("pluginProperties").isEmpty()) {
                formData.addFormError("pluginClass", AppPluginUtil.getMessage("userview.scheduler.pleaseConfigure", SchedulerMenu.class.getName(), SchedulerMenu.MESSAGE_PATH));
                return rows;
            }

            if (row.getId() != null) {
                jobDefinition = jobDefinitionDao.get(row.getId());
            }
            if (jobDefinition == null) {
                jobDefinition = new JobDefinition();
                jobDefinition.setId(UuidGenerator.getInstance().getUuid());
            }

            jobDefinition.setAppId(row.getProperty("applicationId"));
            jobDefinition.setName(row.getProperty("name"));
            jobDefinition.setFrequencyType(row.getProperty("frequencyType"));
            jobDefinition.setHour(row.getProperty("hour"));
            jobDefinition.setMinute(row.getProperty("minute"));
            jobDefinition.setDateOfWeek(row.getProperty("dateOfWeek"));
            jobDefinition.setDayOfMonth(row.getProperty("dayOfMonth"));
            jobDefinition.setSubject(row.getProperty("subject"));
            jobDefinition.setPluginClass(row.getProperty("pluginClass"));
            jobDefinition.setPluginProperties(PropertyUtil.propertiesJsonStoreProcessing(jobDefinition.getPluginProperties(), row.getProperty("pluginProperties")));
            String frequency = row.getProperty("frequencyType");
            String hour = row.getProperty("hour");
            String minute = row.getProperty("minute");
            String dow = row.getProperty("dateOfWeek");
            String dom = row.getProperty("dayOfMonth");

            // validation dasar
            if (hour == null || hour.trim().isEmpty()) {
                hour = "0";
            }

            if (minute == null || minute.trim().isEmpty()) {
                minute = "0";
            }

            row.setProperty("hour", hour);
            row.setProperty("minute", minute);

            String cron = null;

            // DAILY
            if ("daily".equalsIgnoreCase(frequency)) {
                cron = "0 " + minute + " " + hour + " ? * * *";
            }

            // WEEKLY
            else if ("weekly".equalsIgnoreCase(frequency)) {
                if (dow == null || dow.isEmpty()) {
                    formData.addFormError("dateOfWeek", "Please select Day of Week");
                    return rows;
                }
                cron = "0 " + minute + " " + hour + " ? * " + dow + " *";
            }

            // MONTHLY
            else if ("monthly".equalsIgnoreCase(frequency)) {
                if (dom == null || dom.isEmpty()) {
                    formData.addFormError("dayOfMonth", "Please select Day of Month");
                    return rows;
                }
                cron = "0 " + minute + " " + hour + " " + dom + " * ? *";
            }
            else {
                formData.addFormError("frequencyType", "Invalid Frequency");
                return rows;
            }

            // SET KE JOB
            jobDefinition.setTrigger(cron);

            SchedulerUtil.scheduleJob(jobDefinition);
            if (jobDefinition.getNextFireTime() != null) {
                jobDefinitionDao.save(jobDefinition);
            } else {
                formData.addFormError("trigger", AppPluginUtil.getMessage("userview.scheduler.invalidSyntax", SchedulerMenu.class.getName(), SchedulerMenu.MESSAGE_PATH));
            }
        }
        return rows;
    }
}