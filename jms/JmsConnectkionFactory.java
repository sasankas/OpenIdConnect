

package com.nri.xxxxxxxx.inf;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.enums.ValuedEnum;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.nri.xxxxxxxx.inf.exception.xxxxxxxxException;
import com.nri.xxxxxxxx.inf.exception.xxxxxxxxStandardException;
import com.nri.xxxxxxxx.startup.Application;


/**
 * Factory for creating jms connection. The actual implementation
 * of creating a connection is hidden inside this class. Current implementation
 * may also lookup a jms source though JNDI look-up provided the InitialContext is
 * assumed to be set-up by either the application server container or the environment
 * bootstrap loader (for command line application). The factory is itself singleton.
 * <p/>
 *
 * @version     $Revision: 1.17 $, $Date: 2006/06/26 04:52:48 $
 */
public class JmsConnectionFactory
        implements KeyedPoolableObjectFactory {

    //~ Class Attributes =======================================================

    /**
     * The <code>Log</code> instance for this class.
     */
    private static final Log log = LogFactory.getLog(JmsConnectionFactory.class);

    /**
     * The jms default connection configuration file.
     */
    private static final String DEFAULT_CONFIG = "xxxxxxxx-jms.properties";

    /**
     * The singleton instance of this class.
     */
    private static JmsConnectionFactory instance = null;

    //~ Instance Attributes ====================================================

    /**
     * The wrapped connection factory.
     */
    private QueueConnectionFactory mConnectionFactory;

    /**
     * The connection pool.
     */
    private KeyedObjectPool mConnectionPool;

    /**
     * The configuration properties
     */
    private Properties mConfig;

    /**
     * List to hold the user friendly names of the queues
     */
    private Hashtable m_lstDisplayQueue;
    /**
     * MQ Failover error codes
     */
    private HashMap m_MQFailOverErrorCodes = new HashMap();

    //~ Constructors ===========================================================

    /**
     * Construct a default instance
     * and configure using the default configuration
     * property source.
     *
     * @throws xxxxxxxxException indicating failure to init
     */
    protected JmsConnectionFactory()
            throws xxxxxxxxException {
        this(ResourceUtils.loadProperties(DEFAULT_CONFIG));
    }


    /**
     * Construct a default instance.
     *
     * @param prop the Propertues object
     *
     * @throws xxxxxxxxException indicating failure to init
     */
    protected JmsConnectionFactory(Properties prop)
            throws xxxxxxxxException {

        try {
            if (prop == null) {
                throw new IllegalArgumentException("jms configuration properties is NULL");
            }

            log.debug("Creating QueueConnectionFactory ");
            MQQueueConnectionFactory qcf = new MQQueueConnectionFactory();

            // Go on setting the attributes of the queue Manager
            // Set queueManager name
            qcf.setQueueManager(prop.getProperty(Application.JMS_QUEUEMANAGER_NAME_KEY));

            // Set CCSID if specified
            String CCSIDStr = prop.getProperty(Application.JMS_QUEUEMANAGER_CCSID_KEY);
            if(StringUtils.isNotEmpty(CCSIDStr))
                qcf.setCCSID(Integer.parseInt(CCSIDStr));

            // Query transport type
            String transportTypeStr = prop.getProperty(Application.JMS_QUEUEMANAGER_TRANSPORT_KEY);
            TransportType type = TransportType.getTransportType(transportTypeStr);
            if(type == null)
                throw new xxxxxxxxStandardException("invalid transport type specified");
            // Set Transport Type
            qcf.setTransportType(type.getValue());

            // Set Host Name
            qcf.setHostName(prop.getProperty(Application.JMS_QUEUEMANAGER_HOST_KEY));
            // Set port
            qcf.setPort(Integer.parseInt(
                           prop.getProperty(Application.JMS_QUEUEMANAGER_PORT_KEY)));
            // Set Channel
            qcf.setChannel(prop.getProperty(Application.JMS_QUEUEMANAGER_CHANNEL_KEY));
            mConnectionFactory = qcf;

            log.debug("Creating QueueConnection Pool ");
            GenericKeyedObjectPool.Config config = new GenericKeyedObjectPool.Config();
            config.maxActive = Integer
                                      .parseInt(prop
                                                    .getProperty(Application.JMS_POOL_MAXACTIVE_KEY));
            config.maxIdle = Integer
                                    .parseInt(prop
                                                  .getProperty(Application.JMS_POOL_MAXIDLE_KEY));
            config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
            mConnectionPool = new GenericKeyedObjectPool(this, config);

            //Enable validity check on borrow object from pool only if
            // MQ failover is enabled
            if(StringUtils.equalsIgnoreCase("TRUE",
            		prop.getProperty(Application.JMS_FAILOVER_ENABLED_KEY))) {
            	((GenericKeyedObjectPool)mConnectionPool).setTestOnBorrow(true);
            	//build up failover error code map
            	String failoverErrorCodes =
            		prop.getProperty(Application.JMS_FAILOVER_ERRORCODES_KEY);
            	if(failoverErrorCodes != null) {
            		StringTokenizer strTok = new StringTokenizer(failoverErrorCodes, ",");
            		while(strTok.hasMoreTokens()) {
            			String errCode = strTok.nextToken();
            			m_MQFailOverErrorCodes.put(errCode, errCode);
            		}
            	}
            }

            // Cache configuration parameters
            mConfig = (Properties)prop.clone();

        } catch (Exception e) {

            log.fatal("Error loading jms configuration", e);
            throw new xxxxxxxxStandardException(e);
        }

    }


    //~ Class Methods ==========================================================

    /**
     * Get the singleton instance of this factory.
     * <p/>
     *
     * @return  the singleton instance of this factory
     */
    public static JmsConnectionFactory getInstance() {
        return instance;
    }


    /**
     * Initialize the instance by configuring
     * the factory from the default property configuration.
     *
     * @throws xxxxxxxxException indicating failure to init
     */
    public static synchronized void init()
            throws xxxxxxxxException {

        if (instance == null) {
            instance = new JmsConnectionFactory();
        }

    }


    /**
     * Initialize the instance by configuring
     * the factory from the property configuration as
     * provided by the caller in prop argument.
     *
     * @param prop - the configuration properties object from which
     *               the factory will be initialized.
     *
     * @throws xxxxxxxxException indicating failure to init
     */
    public static synchronized void init(Properties prop)
            throws xxxxxxxxException {

        if (instance == null) {
            instance = new JmsConnectionFactory(prop);
        }

    }


    /**
     * Un-initialize the instance.
     */
    public static synchronized void destroy() {

        if (instance != null) {
            instance.cleanup();
            instance = null;
        }

    }


    //~ Instance Methods =================================================


    /**
     * Cleanup this instance.
     */
    private void cleanup() {

        try {
            mConnectionPool.close();
        } catch (Exception ex) {
            // Cannot really do anything, silently bail out
            log.warn("cleanup - failed, exception ignored", ex);
        }
    }

    /**
     * Open a connection.
     *
     * Connections are created so long the pool reaches the maxActive mark.
     * Thereafter either a connection is reused, if available, or the call is blocked.
     *
     *
     * @throws  xxxxxxxxException  wrapping the original root cause
     *
     * @return  javax.jms.QueueConnection for use
     */
    public JmsPoolObject openConnection(boolean transacted)
            throws xxxxxxxxException {

        try {
        	return (JmsPoolObject) mConnectionPool.borrowObject(
            												new Boolean(transacted));
        } catch (Exception e) {
			log.error("Failed to open message queue connection", e);
            throw new xxxxxxxxStandardException(e);
        }

    }


    /**
     * Close a connection.
     *
     * Connections are returned to the pool.
     * On connection pool clean up, connections are physically closed.
     *
     * @param   conn      the connection to be closed
     *
     */
    public void closeConnection(JmsPoolObject poolObj) {

		//For transacted queue connection rollback the connection before
		//returning to the pool
		try {
			if(poolObj.getQueueSession().getTransacted()) {
				poolObj.getQueueSession().rollback();
			}
		} catch (Exception e) {
            log.warn("Failed to rollback message queue session, exception ignored", e);
        }

        try {
        	boolean transacted = poolObj.getQueueSession().getTransacted();
            mConnectionPool.returnObject(new Boolean(transacted), poolObj);
        } catch (Exception e) {
            log.warn("returning a connection to the pool-failed, exception ignored", e);
        }

    }


    /**
     * Connection is created when the pool warms up.
     *
     * @throws Exception indicating failure to do so
     *
     * @return the connection object
     */
    public Object makeObject(Object key)
            throws Exception {

        try {
            QueueConnection conn = mConnectionFactory.createQueueConnection();
            QueueSession session = conn.createQueueSession(
            		((Boolean)key).booleanValue(),
                    QueueSession.AUTO_ACKNOWLEDGE);
            return new JmsPoolObject(conn, session);
        } catch (JMSException e) {
            log.fatal(e);
            throw new xxxxxxxxStandardException(e);
        }

    }


    /**
     * Connection is closed when the pool is cleaned up.
     *
     * @param obj the connection to be destroyed
     */
    public void destroyObject(Object key, Object obj) {

        try {
        	JmsPoolObject poolObj = (JmsPoolObject)obj;
        	poolObj.getQueueSession().close();
        	poolObj.getQueueConnection().close();
        } catch (JMSException e) {
            e.printStackTrace();
            log.warn("close connection - failed, exception ignored", e);
        }

    }


    /**
     * Connection is dafault validated.
     *
     * @param obj the connection object to be validated
     *
     * @return validation
     */
    public boolean validateObject(Object key, Object obj) {
    	try {
    		QueueSession qSession = ((JmsPoolObject)obj).getQueueSession();
    		Queue queue = qSession.createQueue(getTestQueueName());
    		QueueBrowser qBrowser = qSession.createBrowser(queue);
    		qBrowser.getEnumeration();
    		qBrowser.close();
    	}catch(JMSException jmsEx) {
    		log.warn("Failed to create sender", jmsEx);
    		return false;
    	}
    	return true;
    }


    /**
     * Connection is started when returned from pool.
     *
     * @param obj the object to be activated
     *
     *
     * @throws Exception indicating failure to do so
     */
    public void activateObject(Object key, Object obj)
            throws Exception {

        try {
        	JmsPoolObject poolObj = (JmsPoolObject)obj;
            poolObj.getQueueConnection().start();
        } catch (JMSException e) {
            log.fatal(e);
            throw new xxxxxxxxStandardException(e);
        }

    }


    /**
     * Connection is stopped when returned to pool.
     *
     * @param obj to be passivated
     *
     */
    public void passivateObject(Object key, Object obj) {

        try {
        	JmsPoolObject poolObj = (JmsPoolObject)obj;
            poolObj.getQueueConnection().stop();
        } catch (JMSException e) {
            log.warn("stopping a connection - failed, exception ignored", e);
        }

    }

    /**
     * Get a collection of logical queue names for a component.
     * @param module    the module for which the listing is sought
     * @return          a list of logical queue names for the passed component
     */
    public List getLogicalQueueNames(Module module) {

        ArrayList results = new ArrayList();
        m_lstDisplayQueue = new Hashtable();

        Enumeration e = mConfig.propertyNames();
        while(e.hasMoreElements()) {
            String propKey = (String)e.nextElement();
            if(propKey.startsWith(Application.PROP_JMS_QUEUE_NAME) &&
               propKey.indexOf(module.getName())!= -1 &&
               !propKey.endsWith(".jmsClient")) {

                // Extract the last portion
                String logicalName = propKey.substring(propKey.lastIndexOf(".") + 1);
                // populate display name
                m_lstDisplayQueue.put(logicalName, mConfig.get(propKey) );
                results.add(logicalName);
            }
        }
        return results;
    }
    /**
     * Checks a given exception is due to MQ failover or not.
     * @param exception The exception object to be checked
     * @return returns true if the given exception was thrown due to MQ failover
     */
    boolean isMQFailoverException(Exception exception) {
    	if(exception instanceof JMSException) {
    		if(m_MQFailOverErrorCodes.get(
    				((JMSException)exception).getErrorCode()) != null) {
    			return true;
    		}
    	}
    	return false;
    }

    /**
     * Get the queue names for display
     * @return
     */
    public Hashtable getQueueDisplayName() {
    	return m_lstDisplayQueue;
    }

    /**
     * Get the physical queue name for the given logical queue name of the component.
     * @param module    the component associated with queueId
     * @param queueId   the logical queue Id
     * @return          the physical queue name, null if no entry found
     */
    String getTestQueueName() {
        return mConfig.getProperty(Application.PROP_JMS_TEST_QUEUE_NAME);
    }

    /**
     * Get the physical queue name for the given logical queue name of the component.
     * @param module    the component associated with queueId
     * @param queueId   the logical queue Id
     * @return          the physical queue name, null if no entry found
     */
    String getPhysicalQueueName(Module module, String queueId) {

        String key = Application.PROP_JMS_QUEUE_NAME
                        + "."
                        + module.getName()
                        + "."
                        + queueId;

        return mConfig.getProperty(key);
    }


    /**
     * Check whether the queue identified by this ID has "JMSClient" property set.
     * @param module    component associated with queueId
     * @param queueId   the logical queue Id
     * @return          true if the logical queueId has "jmsClient" property set to true,
     *                  false otherwise.
     */
    boolean isQueueJMSClient(Module module, String queueId) {

        String key = Application.PROP_JMS_QUEUE_NAME
                        + "."
                        + module.getName()
                        + "."
                        + queueId
                        + ".jmsClient";

        String jmsClient = StringUtils.trimToNull(mConfig.getProperty(key));

        return BooleanUtils.toBoolean(jmsClient);

    }

    /**
     * Transport Type enumertations for connecting to Queue Manager.
     */
    public static class TransportType extends ValuedEnum {

        /**
         * BIND mode of transport - uses native library call.
         */
        public static final TransportType TRANSPORT_TYPE_BIND =
                  new TransportType("BIND",JMSC.MQJMS_TP_BINDINGS_MQ);

        /**
         * TCP mode of transport - uses TCP/IP in client mode.
         */
        public static final TransportType TRANSPORT_TYPE_CLIENT_TCP =
                  new TransportType("CLIENT_TCP",JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);

        /**
         * Access a typed enum given a string.
         * @param type - the name against which enumertaion will be looked up.
         * @return the named <code>TransportType</code> matching the given name, null if there is none.
         */
        public static TransportType getTransportType(String type) {
            return (TransportType)getEnum(TransportType.class,type);
        }

        /**
         * Private Constructor
         */
        private TransportType(String name, int value) {
            super(name,value);
        }

    }

    static class JmsPoolObject {
    	/**
    	 * Message Queue connection
    	 */
    	private QueueConnection m_qConnection;
    	/**
    	 * Message Queue session
    	 */
    	private QueueSession m_qSession;

    	/**
    	 * Constructor.
    	 * Initializes queue connection and session
    	 */
    	JmsPoolObject(QueueConnection qConn, QueueSession qSess) {
    		m_qConnection = qConn;
    		m_qSession = qSess;
    	}

		QueueConnection getQueueConnection() {
			return m_qConnection;
		}

		void setQueueConnection(QueueConnection connection) {
			m_qConnection = connection;
		}

		QueueSession getQueueSession() {
			return m_qSession;
		}

		void setQueueSession(QueueSession session) {
			m_qSession = session;
		}

		public int hashCode() {
			//picked a hard-coded, randomly chosen, non-zero, odd number
		     // ideally different for each class
		     return new HashCodeBuilder(17, 37).
		       append(m_qConnection).
		       append(m_qSession).
		       toHashCode();
		}

		public boolean equals(Object obj) {
			if (obj instanceof JmsPoolObject == false) {
			     return false;
			   }
			   if (this == obj) {
			     return true;
			   }
			   JmsPoolObject rhs = (JmsPoolObject) obj;
			   return new EqualsBuilder()
			                 .appendSuper(super.equals(obj))
			                 .append(m_qConnection, rhs.m_qConnection)
			                 .append(m_qSession, rhs.m_qSession)
			                 .isEquals();

		}

    }

}
