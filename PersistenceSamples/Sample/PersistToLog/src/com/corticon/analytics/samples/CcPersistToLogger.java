package com.corticon.analytics.samples;

import java.util.List;

import com.corticon.analytics.CcAnalyticsHandler;
import com.corticon.analytics.CcAnalyticsHandlerMetric;
import com.corticon.analytics.CcAnalyticsHandlerRuleMessage;
import com.corticon.log.Log;

public class CcPersistToLogger extends CcAnalyticsHandler
{
   public void processAnalytics()
   {
      StringBuilder lSBResults = new StringBuilder();
      
      //  *** Log Base information
      lSBResults.append("--DecisionService--\n");
      lSBResults.append(getDecisionServiceName() + " version " + "(" + getMajorVersionNumber() + "." + getMinorVersionNumber() + ")" + "\n");

      StringBuilder lSBRuleMessages = new StringBuilder();
      lSBRuleMessages.append("--Messages--\n");

      //  *** Log RuleMessages
      List<CcAnalyticsHandlerRuleMessage> llstRuleMessages = getRuleMessges();
      if (llstRuleMessages.isEmpty())
      {
         lSBRuleMessages.append("No Messages in Output\n");
      }
      else
      {
         int liCounter = 0;
         for (CcAnalyticsHandlerRuleMessage lCcPersistenceRuleMessage : llstRuleMessages)
         {
            liCounter++;
            String lstrText = lCcPersistenceRuleMessage.getText();
            String lstrSeverity = lCcPersistenceRuleMessage.getSeverity();
            
            lSBRuleMessages.append(liCounter + ".  Severity: " + lstrSeverity + "\tText: " + lstrText + "\n");
         }
      }
      lSBResults.append(lSBRuleMessages);

      //  *** Log Metrics
      List<CcAnalyticsHandlerMetric> llstEntityChanges = getEntityChanges();
      List<CcAnalyticsHandlerMetric> llstAttributeChanges = getAttributeChanges();
      List<CcAnalyticsHandlerMetric> llstAssociationChanges = getAssociationChanges();

      StringBuilder lSBMetrics = new StringBuilder();
      lSBMetrics.append("--Metrics--\n");
      
      lSBMetrics.append("Entity Changes\n");
      if (llstEntityChanges.isEmpty())
      {
         lSBMetrics.append("\tNo Entity Changes\n");
      }
      else
      {
         StringBuilder lSBEntityChanges = new StringBuilder();
         int liCounter = 0;
         for (CcAnalyticsHandlerMetric lCcMetric : llstEntityChanges)
         {
            int liSequenceNumber = lCcMetric.getSequenceNumber();
            String lstrAction = lCcMetric.getAction();
            String lstrEntityName = lCcMetric.getEntityName();

            String lstrRulesheetName = lCcMetric.getRulesheetName();
            String lstrRuleNumber = lCcMetric.getRuleNumber();

            lSBEntityChanges.append("\t" + liSequenceNumber + ".  Action: " + lstrAction + "\tEntity Name: " + lstrEntityName + "\tRulesheetRule:" + lstrRulesheetName + ":" + lstrRuleNumber + "\n");
         }
         lSBMetrics.append(lSBEntityChanges);
      }
      
      lSBMetrics.append("Attribute Changes\n");
      if (llstAttributeChanges.isEmpty())
      {
         lSBMetrics.append("\tNo Attribute Changes\n");
      }
      else
      {
         StringBuilder lSBAttributeChanges = new StringBuilder();
         int liCounter = 0;
         for (CcAnalyticsHandlerMetric lCcMetric : llstAttributeChanges)
         {
            int liSequenceNumber = lCcMetric.getSequenceNumber();
            String lstrAction = lCcMetric.getAction();
            String lstrEntityName = lCcMetric.getEntityName();
            String lstrAttributeName = lCcMetric.getAttributeName();
            Object lobjBeforeValue = lCcMetric.getAttributeBeforeValue();
            Object lobjAfterValue = lCcMetric.getAttributeAfterValue();

            String lstrRulesheetName = lCcMetric.getRulesheetName();
            String lstrRuleNumber = lCcMetric.getRuleNumber();

            lSBAttributeChanges.append("\t" + liSequenceNumber + ".  Action: " + lstrAction + "\tEntity Name: " + lstrEntityName + "\tAttribute Name: " + lstrAttributeName  + "\tBefore Value: " + lobjBeforeValue + "\tAfter Value: " + lobjAfterValue + "\tRulesheet Rule: " + lstrRulesheetName + ":" + lstrRuleNumber + "\n");
         }
         lSBMetrics.append(lSBAttributeChanges);
      }

      lSBMetrics.append("Association Changes\n");
      if (llstAssociationChanges.isEmpty())
      {
         lSBMetrics.append("\tNo Association Changes\n");
      }
      else
      {
         StringBuilder lSBAttributeChanges = new StringBuilder();
         int liCounter = 0;
         for (CcAnalyticsHandlerMetric lCcMetric : llstAssociationChanges)
         {
            int liSequenceNumber = lCcMetric.getSequenceNumber();
            String lstrAction = lCcMetric.getAction();
            String lstrEntityName = lCcMetric.getEntityName();
            String lstrTargetEntityName = lCcMetric.getTargetEntityName();
            String lobjRoleName = lCcMetric.getRoleName();

            String lstrRulesheetName = lCcMetric.getRulesheetName();
            String lstrRuleNumber = lCcMetric.getRuleNumber();

            lSBAttributeChanges.append("\t" + liSequenceNumber + ".  Action: " + lstrAction + "\tEntity Name: " + lstrEntityName + "\tTarget Entity Name: " + lstrTargetEntityName  + "\tRole Name: " + lobjRoleName + "\tRulesheet Rule: " + lstrRulesheetName + ":" + lstrRuleNumber + "\n");
         }
         lSBMetrics.append(lSBAttributeChanges);
      }
      lSBResults.append(lSBMetrics);
      
      Log.logDiagnostic("CcPersistToLogger Results:\n" + lSBResults.toString(), this);
   }
}
