/*
 * Copyright 2005-2022 Progress Software Corporation and/or its subsidiaries and affiliates. All rights reserved.
 */
package com.corticon.analytics.samples;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.jdom.Document;
import org.json.JSONException;
import org.json.JSONObject;

import com.corticon.analytics.CcAnalyticsHandler;
import com.corticon.analytics.CcAnalyticsHandlerMetric;
import com.corticon.analytics.CcAnalyticsHandlerRuleCatalog;
import com.corticon.analytics.CcAnalyticsHandlerRuleMessage;
import com.corticon.init.CcProperties;
import com.corticon.log.Log;
import com.corticon.util.CcUtil;
import com.corticon.util.CcXmlIO;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public class CcPersistToDatabase extends CcAnalyticsHandler
{
   private static final String SQL_TYPE_INSERT_DECISION_SERVICE = "SQL_TYPE_INSERT_DECISION_SERVICE";
   private static final String SQL_TYPE_INSERT_RULE_CATALOG     = "SQL_TYPE_INSERT_RULE_CATALOG";
   private static final String SQL_TYPE_INSERT_EXECUTION        = "SQL_TYPE_INSERT_EXECUTION";
   private static final String SQL_TYPE_INSERT_RULE_AUDITING    = "SQL_TYPE_INSERT_RULE_AUDITING";
   private static final String SQL_TYPE_INSERT_RULE_EXECUTION   = "SQL_TYPE_INSERT_RULE_EXECUTION";
   private static final String SQL_TYPE_INSERT_PAYLOAD          = "SQL_TYPE_INSERT_PAYLOAD";
   private static final String SQL_TYPE_INSERT_RULE_MESSAGES    = "SQL_TYPE_INSERT_RULE_MESSAGES";

   private static final String SQL_TYPE_READ_DECISION_SERVICE   = "SQL_TYPE_READ_DECISION_SERVICE";
   private static final String SQL_TYPE_READ_RULE_CATALOG       = "SQL_TYPE_READ_RULE_CATALOG";

   private static final boolean DEBUG = CcProperties.getCcProperty("CcPersistToDatabaseJSON.debug", "false").equals("true") ? true : false;
   
   private Properties lpropDatabaseConnection = null;

   public void processAnalytics()
   {
      long llStartTime = System.currentTimeMillis();

      if (DEBUG)
         CcUtil.writeToSystemOutPrintln("====== CcPersistToDatabaseJSON (Start) ======");

      Log.logDebug("CcPersistToDatabase.call() Start", this);

      try
      {
         InputStream lInputStream = getClass().getClassLoader().getResourceAsStream("CcPersistToDatabase.properties");
         if (lInputStream == null)
         {
            //  Log that nothing was found...
            return;
         }
         
         lpropDatabaseConnection = new Properties();
         lpropDatabaseConnection.load(lInputStream);
         
         Log.logDebug("CcPersistToDatabase.call() Found properties == " + lpropDatabaseConnection, this);
         
         Connection lConnection = getConnection();
         
         Object lobjInput = getInput();
         Object lobjOutput = getOutput();
         
         List<CcAnalyticsHandlerRuleCatalog> llstRuleCatatlog = getRuleCatalog();
         List<CcAnalyticsHandlerRuleMessage> llstRuleMessages = getRuleMessges();
         
         List<CcAnalyticsHandlerMetric> llstEntityChanges = getEntityChanges();
         List<CcAnalyticsHandlerMetric> llstAttributeChanges = getAttributeChanges();
         List<CcAnalyticsHandlerMetric> llstAssociationChanges = getAssociationChanges();

         //  Create a list of all the changes
         List<CcAnalyticsHandlerMetric> llstAllChanges = new ArrayList();
         llstAllChanges.addAll(llstEntityChanges);
         llstAllChanges.addAll(llstAttributeChanges);
         llstAllChanges.addAll(llstAssociationChanges);
         
         //-------------------------------------------------------------------------------------------------
         //  Get an existing Decision Service Id from the Database, if it is there.
         Integer liDecisionServiceId = getDecisionServiceIdFromDatabase(lConnection);
         
         //  If the Decision Service hasn't already been added to the Database, we need to do that now, and reuse the "id"
         //  in subsequent operations
         if (liDecisionServiceId == null)
            liDecisionServiceId = insertIndividualDecisionServiceIntoDatabase(lConnection);
         
         //  If we don't have a valid DecisionService id value, then there is no need to continue.
         if (liDecisionServiceId == null)
         {
            Log.logError("Failed to establish a Decision Service id.  Opertation terminated.", this);
            return;
         }

         //-------------------------------------------------------------------------------------------------
         // Get the Rule Catalog from the Database associated with this Decision Service.
         HashMap<String, Integer> lhmRuleCatalogRulesheetRuleToDatabaseId = getRuleCatalogIdsFromDatabase(lConnection, liDecisionServiceId);
         
         //  If there is nothing in the Database, then we need to add the current Rule Catalog to the Database and retain 
         //  the "id" value for each Rule entered
         if (lhmRuleCatalogRulesheetRuleToDatabaseId.isEmpty())
         {
            lhmRuleCatalogRulesheetRuleToDatabaseId = insertIndividualRuleCatalogIntoDatabase(lConnection, liDecisionServiceId, llstRuleCatatlog);
         }
         else
         {
            //  There are Rule Catalogs in the Database.  Now compare those values with what is associated with the Decision Service.
            //  If there are Rules associated with the Decision Service that are not in the Database, then we need to stop execution.
            //  The Database values are out-of-sync with the Decision Service.
            boolean lbMatchDecisionServiceRulesToDatabaseRuleCatalog = validateRuleCatalogWithDecisionService(lhmRuleCatalogRulesheetRuleToDatabaseId, llstRuleCatatlog);
            if (lbMatchDecisionServiceRulesToDatabaseRuleCatalog == false)
            {
               Log.logError("Rule Catalog in Database for Decision Service Id (" + liDecisionServiceId + ") is out-of-sync with Decision Service.  Opertation terminated.", this);
               return;
            }
         }
         
         //  Again, if we don't have the necessary Rule Catalog Ids, then there is no need to continue.
         if (lhmRuleCatalogRulesheetRuleToDatabaseId.isEmpty())
         {
            Log.logError("Failed to establish Rule Catalog ids.  Opertation terminated.", this);
            return;
         }
         
         //-------------------------------------------------------------------------------------------------
         //  Insert a new Execution record into the Database for this Decision Service and retain the "id" value
         //  to be used in subsequent operations.
         long llExecutionStartTime = getExecutionStartTime();
         long llTotalExecutionTime = getExecutionTotalTime();

         int liExecutionId = insertIndividualExecutionIntoDatabase(lConnection, liDecisionServiceId, llExecutionStartTime, llTotalExecutionTime);
         
         //  Insert Rule Auditing records.  Since we don't need to reuse the new "id" value for each record, this operation can be done 
         //  in Batch mode for performance reasons.
         insertBatchRuleAuditingIntoDatabase(lConnection, llstAllChanges, liExecutionId, lhmRuleCatalogRulesheetRuleToDatabaseId);

         //  Insert Rule Execution records.  Since we don't need to reuse the new "id" value for each record, this operation can be done 
         //  in Batch mode for performance reasons.
         insertBatchRuleExecutionsIntoDatabase(lConnection, llstAllChanges, liExecutionId, lhmRuleCatalogRulesheetRuleToDatabaseId);
         
         //  Record Input and Output Payloads
         insertIndividualPayloadsIntoDatabase(lConnection, lobjInput, lobjOutput, liExecutionId);
         
         //  Record RuleMessages
         insertBatchRuleMessagesIntoDatabase(lConnection, llstRuleMessages, liExecutionId);
      }
      catch (IOException | JSONException | SQLException | PropertyVetoException e)
      {
         Log.logException(e, "Unexpected Error when persisting to database.", this);
         return;
      }
      catch (Exception e)
      {
         Log.logException(e, "Unexpected Error when persisting to database.", this);
         return;
      }
      finally
      {
         //  Clear out all instance variables inside this ancestor classes
         clear();
         
         long llStopTime = System.currentTimeMillis();
         if (DEBUG)
            CcUtil.writeToSystemOutPrintln("*** CcPersistToDatabaseJSON Total Time = " + (llStopTime - llStartTime));
         
         Log.logRuleTrace("CcPersistToDatabase.call() Total Time = " + (llStopTime - llStartTime), this);
         Log.logTiming("CcPersistToDatabase.call() Total Time = " + (llStopTime - llStartTime), this);
      }
   }
   
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   private Integer getDecisionServiceIdFromDatabase(Connection aConnection)
   {
      long llStartTime = System.currentTimeMillis();

      Integer liReturnId = null;
      
      String  lstrDecisionServiceName = getDecisionServiceName();
      Integer liMajorVersionNumber    = getMajorVersionNumber();
      Integer liMinorVersionNumber    = getMinorVersionNumber();
      
      if (lstrDecisionServiceName != null && liMajorVersionNumber != null && liMinorVersionNumber != null)
      {
         String lstrSqlRead = getSqlStatement(SQL_TYPE_READ_DECISION_SERVICE);
         try (PreparedStatement lPreparedStatement = aConnection.prepareStatement(lstrSqlRead))
         {
            lPreparedStatement.setObject(1, lstrDecisionServiceName);
            lPreparedStatement.setObject(2, liMajorVersionNumber);
            lPreparedStatement.setObject(3, liMinorVersionNumber);

            lPreparedStatement.execute();

            ResultSet lResultSet = lPreparedStatement.getResultSet();
            while (lResultSet.next())
            {
               liReturnId = lResultSet.getInt(1);
               break;
            }
         }
         catch (SQLException e)
         {
            Log.logException(e, "Failed to get Decision Service 'id' from Database", this);
         }
      }
      
      long llStopTime = System.currentTimeMillis();
      if (DEBUG)
         CcUtil.writeToSystemOutPrintln("CcPersistToDatabaseJSON.getDecisionServiceIdFromDatabase() = " + (llStopTime - llStartTime));

      return liReturnId;
   }

   private Integer insertIndividualDecisionServiceIntoDatabase(Connection aConnection) throws SQLException
   {
      long llStartTime = System.currentTimeMillis();

      Integer liReturnId = null;

      String  lstrDecisionServiceName = getDecisionServiceName();
      Integer liMajorVersionNumber    = getMajorVersionNumber();
      Integer liMinorVersionNumber    = getMinorVersionNumber();

      if (lstrDecisionServiceName != null && liMajorVersionNumber != null && liMinorVersionNumber != null)
      {
         //  If we didn't return from the previous block that means that a row didn't exists.
         //  Now we need to add a new row and get the GeneratedKwy from the Insert.
         String lstrSqlInsert = getSqlStatement(SQL_TYPE_INSERT_DECISION_SERVICE);
         try (PreparedStatement lPreparedStatement = aConnection.prepareStatement(lstrSqlInsert, Statement.RETURN_GENERATED_KEYS))
         {
            lPreparedStatement.setObject(1, lstrDecisionServiceName);
            lPreparedStatement.setObject(2, liMajorVersionNumber);
            lPreparedStatement.setObject(3, liMinorVersionNumber);

            lPreparedStatement.execute();
            aConnection.commit();

            ResultSet lResultSet = lPreparedStatement.getGeneratedKeys();
            while (lResultSet.next())
            {
               liReturnId = lResultSet.getInt(1);
               break;
            }
         }
         catch (SQLException e)
         {
            Log.logException(e, "Failed to insert individual Decision Service into Database", this);
            aConnection.rollback();
         }
      }
      
      long llStopTime = System.currentTimeMillis();
      if (DEBUG)
         CcUtil.writeToSystemOutPrintln("CcPersistToDatabaseJSON.insertIndividualDecisionServiceIntoDatabase() = " + (llStopTime - llStartTime));

      return liReturnId;
   }

   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   private HashMap<String, Integer> getRuleCatalogIdsFromDatabase(Connection aConnection, int aiDecisionServiceId)
   {
      long llStartTime = System.currentTimeMillis();

      HashMap<String, Integer> lhmRuleCatalogRulesheetRuleToDatabaseId = new HashMap();
      
      String lstrSqlRead = getSqlStatement(SQL_TYPE_READ_RULE_CATALOG);
      try (PreparedStatement lPreparedStatement = aConnection.prepareStatement(lstrSqlRead))
      {
         lPreparedStatement.setObject(1, aiDecisionServiceId);

         lPreparedStatement.execute();

         ResultSet lResultSet = lPreparedStatement.getResultSet();
         while (lResultSet.next())
         {
            int liId = lResultSet.getInt(1);
            String lstrRulesheetName = lResultSet.getString(2);
            String lstrRuleNumber = lResultSet.getString(3);
            
            lhmRuleCatalogRulesheetRuleToDatabaseId.put(lstrRulesheetName + "-" + lstrRuleNumber, liId);
         }
      }
      catch (SQLException e)
      {
         Log.logException(e, "Failed to get Rule Catalog 'ids' from Database", this);
      }

      long llStopTime = System.currentTimeMillis();
      if (DEBUG)
         CcUtil.writeToSystemOutPrintln("CcPersistToDatabaseJSON.getRuleCatalogIdsFromDatabase() = " + (llStopTime - llStartTime));

      return lhmRuleCatalogRulesheetRuleToDatabaseId;
   }

   private HashMap<String, Integer> insertIndividualRuleCatalogIntoDatabase(Connection aConnection, int aiDecisionServiceId, List<CcAnalyticsHandlerRuleCatalog> alstRuleCatatog) throws JSONException, SQLException
   {
      long llStartTime = System.currentTimeMillis();

      HashMap<String, Integer> lhmRuleCatalogRulesheetRuleToDatabaseId = new HashMap();
      
      //  If there is nothing inside the lhmRuleCatalog, then there is nothing in the Database.  In this case, we need to insert
      //  new rows and get the generated key value.
      String lstrSqlInsert = getSqlStatement(SQL_TYPE_INSERT_RULE_CATALOG);
         
      //  Loop through each Rule Catalog and insert a new row
      for (CcAnalyticsHandlerRuleCatalog lCcAnalyticsRuleCatalog : alstRuleCatatog)
      {
         String lstrRulesheetName = lCcAnalyticsRuleCatalog.getRuleSheetName();
         String lstrRuleNumber = lCcAnalyticsRuleCatalog.getRuleNumber();

         try (PreparedStatement lPreparedStatement = aConnection.prepareStatement(lstrSqlInsert, Statement.RETURN_GENERATED_KEYS))
         {
            lPreparedStatement.setObject(1, aiDecisionServiceId);
            lPreparedStatement.setObject(2, lstrRulesheetName);
            lPreparedStatement.setObject(3, lstrRuleNumber);

            lPreparedStatement.execute();
            aConnection.commit();

            ResultSet lResultSet = lPreparedStatement.getGeneratedKeys();
            while (lResultSet.next())
            {
               int liId = lResultSet.getInt(1);
               
               // Add to HashSet the RuleLabel for this RuleCatalog
               lhmRuleCatalogRulesheetRuleToDatabaseId.put(lstrRulesheetName + "-" + lstrRuleNumber, liId);
               break;
            }
         }
         catch (SQLException e)
         {
            Log.logException(e, "Failed to insert individual Rule Catalog into Database", this);
            aConnection.rollback();
         }
      }
      
      long llStopTime = System.currentTimeMillis();
      if (DEBUG)
         CcUtil.writeToSystemOutPrintln("CcPersistToDatabaseJSON.insertIndividualRuleCatalogIntoDatabase() = " + (llStopTime - llStartTime));

      return lhmRuleCatalogRulesheetRuleToDatabaseId;
   }
   
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   private Integer insertIndividualExecutionIntoDatabase(Connection aConnection, int aiDecisionServiceId, long alExecutionStartTime, long alExecutionTotalTime) throws SQLException
   {
      long llStartTime = System.currentTimeMillis();

      Integer liReturnId = null;

      String lstrSqlInsert = getSqlStatement(SQL_TYPE_INSERT_EXECUTION);
      try (PreparedStatement lPreparedStatement = aConnection.prepareStatement(lstrSqlInsert, Statement.RETURN_GENERATED_KEYS))
      {
         lPreparedStatement.setObject(1, aiDecisionServiceId);
         lPreparedStatement.setObject(2, new java.sql.Timestamp(alExecutionStartTime));
         lPreparedStatement.setObject(3, alExecutionTotalTime);

         lPreparedStatement.execute();
         aConnection.commit();

         ResultSet lResultSet = lPreparedStatement.getGeneratedKeys();
         while (lResultSet.next())
         {
            liReturnId = lResultSet.getInt(1);
            break;
         }
      }
      catch (SQLException e)
      {
         Log.logException(e, "Failed to insert individual Execution into Database", this);
         aConnection.rollback();
      }
      
      long llStopTime = System.currentTimeMillis();
      if (DEBUG)
         CcUtil.writeToSystemOutPrintln("CcPersistToDatabaseJSON.insertIndividualExecutionIntoDatabase() = " + (llStopTime - llStartTime));

      return liReturnId;
   }
   
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   private void insertBatchRuleAuditingIntoDatabase(Connection aConnection, List<CcAnalyticsHandlerMetric> alstMetrics, int aiExecutionId, HashMap<String, Integer> ahmRuleCatalogRulesheetRuleToDatabaseId) throws JSONException, SQLException
   {
      long llStartTime = System.currentTimeMillis();

      String lstrSqlInsert = getSqlStatement(SQL_TYPE_INSERT_RULE_AUDITING);
      
      if (alstMetrics == null)
         return;
      
      try (PreparedStatement lPreparedStatement = aConnection.prepareStatement(lstrSqlInsert))
      {
         //  Loop through each Rule Catalog and add a new Batch entry into the PreparedStatement
         for (CcAnalyticsHandlerMetric lCcAnalyticsMetric : alstMetrics)
         {
            int liSequenceNumber = lCcAnalyticsMetric.getSequenceNumber();
            String lstrAction = lCcAnalyticsMetric.getAction();
            
            String lstrEntityName = lCcAnalyticsMetric.getEntityName();
            String lstrEntityIdentityValues = lCcAnalyticsMetric.getEntityIdentityValues();

            String lstrAttributeName = lCcAnalyticsMetric.getAttributeName();

            Object lobjAttributeBeforeValue = lCcAnalyticsMetric.getAttributeBeforeValue();
            Object lobjAttributeAfterValue = lCcAnalyticsMetric.getAttributeAfterValue();

            String lstrRoleName = lCcAnalyticsMetric.getRoleName();
            String lstrTargetEntityName = lCcAnalyticsMetric.getTargetEntityName();
            String lstrTargetEntityIdentityValues = lCcAnalyticsMetric.getTargetEntityIdentityValues();
            
            String lstrRulesheetName = lCcAnalyticsMetric.getRulesheetName();
            String lstrRuleNumber = lCcAnalyticsMetric.getRuleNumber();
            
            // Look up the RuleCatalogId
            String lstrLookupRulesheetRuleId = lstrRulesheetName + "-" + lstrRuleNumber;
            Object lobjRuleCatalogId = ahmRuleCatalogRulesheetRuleToDatabaseId.get(lstrLookupRulesheetRuleId);

            //  Default to null.  IF the lobjRuleCatalogId is null, then this means that Catalog in the Database is out of sync with the 
            //  the Decision Service.  In this case, set NULL in the Database, and log a Warning
            Integer liRuleCatalogId = null;
            
            if (lobjRuleCatalogId != null)
               liRuleCatalogId = (Integer)lobjRuleCatalogId;
            else
               Log.logWarning("Rule Catalog Id not found in Database for (Rulehseet-RuleNumber) : (" + lstrLookupRulesheetRuleId +")", this);
            
            lPreparedStatement.setObject(1,  aiExecutionId);
            lPreparedStatement.setObject(2,  liRuleCatalogId);
            lPreparedStatement.setObject(3,  liSequenceNumber);
            lPreparedStatement.setObject(4,  lstrAction);
            lPreparedStatement.setObject(5,  lstrEntityName);
            lPreparedStatement.setObject(6,  lstrEntityIdentityValues);
            lPreparedStatement.setObject(7,  lstrAttributeName);
            lPreparedStatement.setObject(8,  lobjAttributeBeforeValue);
            lPreparedStatement.setObject(9,  lobjAttributeAfterValue);
            lPreparedStatement.setObject(10, lstrRoleName);
            lPreparedStatement.setObject(11, lstrTargetEntityName);
            lPreparedStatement.setObject(12, lstrTargetEntityIdentityValues);

            //  Add this entry into the Batch
            lPreparedStatement.addBatch();
         }
         
         //  Execute the PreparedStatement
         lPreparedStatement.executeBatch();
         aConnection.commit();
      }
      catch (SQLException e)
      {
         Log.logException(e, "Failed to insert batch Rule Auditing into Database", this);
         aConnection.rollback();
      }
      
      long llStopTime = System.currentTimeMillis();
      if (DEBUG)
         CcUtil.writeToSystemOutPrintln("CcPersistToDatabaseJSON.insertBatchRuleAuditingIntoDatabase() = " + (llStopTime - llStartTime));
   }

   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   private void insertBatchRuleExecutionsIntoDatabase(Connection aConnection, List<CcAnalyticsHandlerMetric> alstMetrics, int aiExecutionId, HashMap<String, Integer> ahmRuleCatalogRulesheetRuleToDatabaseId) throws JSONException, SQLException
   {
      long llStartTime = System.currentTimeMillis();

      String lstrSqlInsert = getSqlStatement(SQL_TYPE_INSERT_RULE_EXECUTION);
      
      if (alstMetrics == null)
         return;

      HashMap<Integer, Integer> lhmRuleCatalogIdToCounter = new HashMap();
      
      //  Loop through all the Rules that fired and count them up
      for (CcAnalyticsHandlerMetric lCcAnalyticsMetric : alstMetrics)
      {
         String lstrRulesheetName = lCcAnalyticsMetric.getRulesheetName();
         String lstrRuleNumber = lCcAnalyticsMetric.getRuleNumber();
         
         // Look up the RuleCatalogId
         int liRuleCatalogId = ahmRuleCatalogRulesheetRuleToDatabaseId.get(lstrRulesheetName + "-" + lstrRuleNumber);

         //  Get current count
         Integer lICurrentCountForRule = lhmRuleCatalogIdToCounter.get(liRuleCatalogId);
         if (lICurrentCountForRule == null)
            lICurrentCountForRule = Integer.valueOf(0);
         
         //  Increase and store back into HashMap
         lICurrentCountForRule = Integer.valueOf(lICurrentCountForRule + 1);
         
         lhmRuleCatalogIdToCounter.put(liRuleCatalogId, lICurrentCountForRule);
      }
      
      //  Now that all the Rules have been counted, add them to the database
      try (PreparedStatement lPreparedStatement = aConnection.prepareStatement(lstrSqlInsert))
      {
         for (Entry<Integer, Integer> lEntry : lhmRuleCatalogIdToCounter.entrySet())
         {
            Integer liRuleCatalogId = lEntry.getKey();
            Integer liRuleCount = lEntry.getValue();

            lPreparedStatement.setObject(1,  aiExecutionId);
            lPreparedStatement.setObject(2,  liRuleCatalogId);
            lPreparedStatement.setObject(3,  liRuleCount);
            
            lPreparedStatement.addBatch();
         }
         lPreparedStatement.executeBatch();
         aConnection.commit();
      }
      catch (SQLException e)
      {
         Log.logException(e, "Failed to insert batch Rule Executions into Database", this);
         aConnection.rollback();
      }

      long llStopTime = System.currentTimeMillis();
      if (DEBUG)
         CcUtil.writeToSystemOutPrintln("CcPersistToDatabaseJSON.insertBatchRuleExecutionsIntoDatabase() = " + (llStopTime - llStartTime));
   }

   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   private void insertIndividualPayloadsIntoDatabase(Connection aConnection, Object aobjInput, Object aobjOutput, int aiExecutionId) throws SQLException
   {
      long llStartTime = System.currentTimeMillis();

      String lstrSqlInsert = getSqlStatement(SQL_TYPE_INSERT_PAYLOAD);
      
      String lstrInput = convertObjectToString(aobjInput);
      String lstrOutput = convertObjectToString(aobjOutput);
      
      try (PreparedStatement lPreparedStatement = aConnection.prepareStatement(lstrSqlInsert))
      {
         lPreparedStatement.setObject(1,  aiExecutionId);
         lPreparedStatement.setObject(2,  lstrInput);
         lPreparedStatement.setObject(3,  lstrOutput);

         lPreparedStatement.execute();
         aConnection.commit();
      }
      catch (SQLException e)
      {
         Log.logException(e, "Failed to insert payloads into Database", this);
         aConnection.rollback();
      }

      long llStopTime = System.currentTimeMillis();
      if (DEBUG)
         CcUtil.writeToSystemOutPrintln("CcPersistToDatabaseJSON.insertIndividualPayloadsIntoDatabase() = " + (llStopTime - llStartTime));
   }

   private String convertObjectToString(Object aobjObject)
   {
      if (aobjObject == null)
         return null;
      
      try
      {
         if (aobjObject instanceof Document)
         {
            Document ldoc = (Document)aobjObject;
            String lstrString = CcXmlIO.fromJDomToString(ldoc);
            
            return lstrString;
         }
         else if (aobjObject instanceof JSONObject)
         {
            JSONObject ljsonObject = (JSONObject)aobjObject;
            String lstrString = ljsonObject.toString();
            
            return lstrString;
         }
      }
      catch (Exception e)
      {
         Log.logException(e, "Failed to convert object into String.", this);
      }

      return null;
   }
   
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   private void insertBatchRuleMessagesIntoDatabase(Connection aConnection, List<CcAnalyticsHandlerRuleMessage> alstRuleMessages, int aiExecutionId) throws JSONException, SQLException
   {
      long llStartTime = System.currentTimeMillis();

      String lstrSqlInsert = getSqlStatement(SQL_TYPE_INSERT_RULE_MESSAGES);
      
      if (alstRuleMessages == null)
         return;

      try (PreparedStatement lPreparedStatement = aConnection.prepareStatement(lstrSqlInsert))
      {
         for (CcAnalyticsHandlerRuleMessage lCcAnalyticsRuleMessage : alstRuleMessages)
         {
            String lstrSeverity = lCcAnalyticsRuleMessage.getSeverity();
            String lstrText = lCcAnalyticsRuleMessage.getText();
            String lstrEntityReference = lCcAnalyticsRuleMessage.getEntityReference();

            lPreparedStatement.setObject(1, aiExecutionId);
            lPreparedStatement.setObject(2, lstrSeverity);
            lPreparedStatement.setObject(3, lstrText);
            lPreparedStatement.setObject(4, lstrEntityReference);

            lPreparedStatement.addBatch();
         }
         lPreparedStatement.executeBatch();

         aConnection.commit();
      }
      catch (SQLException e)
      {
         Log.logException(e, "Failed to insert payloads into Database", this);
         aConnection.rollback();
      }

      long llStopTime = System.currentTimeMillis();
      if (DEBUG)
         CcUtil.writeToSystemOutPrintln("CcPersistToDatabaseJSON.insertBatchRuleMessagesIntoDatabase() = " + (llStopTime - llStartTime));
   }

   //********************************************************************************************************
   //********************************************************************************************************
   //********************************************************************************************************
   private Connection getConnection() throws PropertyVetoException, SQLException
   {
      long llStartTime = System.currentTimeMillis();
      
      String lstrDriverClass = lpropDatabaseConnection.getProperty("driverClass");
      String lstrUrl         = lpropDatabaseConnection.getProperty("databaseUrl");
      String lstrUsername    = lpropDatabaseConnection.getProperty("username");
      String lstrPassword    = lpropDatabaseConnection.getProperty("password");
      
      Connection lConnection = null;

      //  Create a new C3PO Pool
      ComboPooledDataSource lComboPooledDataSource = new ComboPooledDataSource();
      
      lComboPooledDataSource.setDriverClass(lstrDriverClass);
      lComboPooledDataSource.setJdbcUrl(lstrUrl);
      
      // Only set the username/password properties if they are not null
      if (lstrUsername != null)
         lComboPooledDataSource.setUser(lstrUsername);
      if (lstrPassword != null)
         lComboPooledDataSource.setPassword(lstrPassword);

      lComboPooledDataSource.setMaxIdleTime(Integer.parseInt("1800"));
      lComboPooledDataSource.setMaxPoolSize(Integer.parseInt("100"));
      lComboPooledDataSource.setMinPoolSize(Integer.parseInt("1"));
      lComboPooledDataSource.setMaxStatements(Integer.parseInt("50"));
      
      lConnection = lComboPooledDataSource.getConnection();
      lConnection.setAutoCommit(false);

      long llStopTime = System.currentTimeMillis();
      if (DEBUG)
         CcUtil.writeToSystemOutPrintln("CcPersistToDatabaseJSON.getConnection() = " + (llStopTime - llStartTime));

      return lConnection;
   }

   private String getSqlStatement(String astrSqlType)
   {
      String lstrSQL = null;
      if (astrSqlType.equals(SQL_TYPE_INSERT_DECISION_SERVICE))
      {
         lstrSQL = "INSERT into CORTICON_ANALYTICS_DECISION_SERVICE (name, major_version_number, minor_version_number) VALUES (?, ?, ?)";
      }
      else if (astrSqlType.equals(SQL_TYPE_INSERT_RULE_CATALOG))
      {
         lstrSQL = "INSERT into CORTICON_ANALYTICS_RULE_CATALOG (decision_service_id, rulesheet_name, rule_number) VALUES (?, ?, ?)";
      }
      else if (astrSqlType.equals(SQL_TYPE_INSERT_EXECUTION))
      {
         lstrSQL = "INSERT into CORTICON_ANALYTICS_EXECUTION (decision_service_id, execution_timestamp, total_time) VALUES (?, ?, ?)";
      }
      else if (astrSqlType.equals(SQL_TYPE_INSERT_RULE_AUDITING))
      {
         lstrSQL = "INSERT into CORTICON_ANALYTICS_RULE_AUDITING (execution_id, rule_catalog_id, sequence_number, action, entity_name, entity_identity_values, attribute_name, attribute_old_value, attribute_new_value, entity_role_name, association_entity_name, association_entity_identity_values) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      }
      else if (astrSqlType.equals(SQL_TYPE_INSERT_RULE_EXECUTION))
      {
         lstrSQL = "INSERT into CORTICON_ANALYTICS_RULE_EXECUTION (execution_id, rule_catalog_id, count) VALUES (?, ?, ?)";
      }
      else if (astrSqlType.equals(SQL_TYPE_INSERT_PAYLOAD))
      {
         lstrSQL = "INSERT into CORTICON_ANALYTICS_PAYLOADS (execution_id, input, output) VALUES (?, ?, ?)";
      }
      else if (astrSqlType.equals(SQL_TYPE_INSERT_RULE_MESSAGES))
      {
         lstrSQL = "INSERT into CORTICON_ANALYTICS_RULE_MESSAGES (execution_id, severity, text, entity_reference) VALUES (?, ?, ?, ?)";
      }
      else if (astrSqlType.equals(SQL_TYPE_READ_DECISION_SERVICE))
      {
         lstrSQL = "SELECT id FROM CORTICON_ANALYTICS_DECISION_SERVICE WHERE name=? AND major_version_number=? AND minor_version_number=?";
      }
      else if (astrSqlType.equals(SQL_TYPE_READ_RULE_CATALOG))
      {
         lstrSQL = "SELECT id, rulesheet_name, rule_number FROM CORTICON_ANALYTICS_RULE_CATALOG WHERE decision_service_id=?";
      }
      
      return lstrSQL;
   }

   private boolean validateRuleCatalogWithDecisionService(HashMap<String, Integer> ahmRuleCatalogRulesheetRuleToDatabaseId, List<CcAnalyticsHandlerRuleCatalog> alstRuleCatalog)
   {
      //  IF there are no Rules associated with this Decision Service, then just kick out.
      if (alstRuleCatalog == null || alstRuleCatalog.size() == 0)
         return true;
      
      //  Parse the list of CcPersistenceRuleCatalog to see if each Rule has a mapped RuleId
      for (CcAnalyticsHandlerRuleCatalog lCcAnalyticsRuleCatalog : alstRuleCatalog)
      {
         if (lCcAnalyticsRuleCatalog == null)
            continue;
         
         String lstrRulesheetName = lCcAnalyticsRuleCatalog.getRuleSheetName();
         String lstrRuleNumber = lCcAnalyticsRuleCatalog.getRuleNumber();
         
         String lstrRuleCatalogName = lstrRulesheetName + "-" + lstrRuleNumber;
         
         Integer liRuleCatalogId = ahmRuleCatalogRulesheetRuleToDatabaseId.get(lstrRuleCatalogName);
         
         //  If the RuleCatalogId is null, then there is a mismatch.
         if (liRuleCatalogId == null)
         {
            Log.logError("validateRuleCatalogWithDecisionService() : Rule Catalog Id not found in Database for (Rulehseet-RuleNumber) : (" + lstrRuleCatalogName +")", this);
            return false;
         }
      }
      
      return true;
   }
   
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
   //***********************************************************************************************************************************
}