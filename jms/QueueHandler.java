
package com.nri.xxxxxxxx.inf;


import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQQueue;
import com.nri.xxxxxxxx.Globals;
import com.nri.xxxxxxxx.inf.exception.xxxxxxxxException;
import com.nri.xxxxxxxx.inf.exception.xxxxxxxxStandardException;
import com.nri.xxxxxxxx.inf.spi.IMessageProtocol;
import com.nri.xxxxxxxx.startup.Application;
import com.nri.xxxxxxxx.startup.IContext;
import com.nri.xxxxxxxx.startup.IModuleContext;


/**
 * Handler for creating IQueueSenders and IQueueReceivers. The actual implementation
 * of creating a session out of a connection is hidden inside this class. 
 * Current implementation may also lookup a jms queue though JNDI look-up provided 
 * the InitialContext is assumed to be set-up by either the application 
 * server container or the environment bootstrap loader (for command line application). 
 * <p/>
 *
 * QueueHandler is not multi-thread safe - since Session is a single threaded view 
 * of a jms connection and QueueHandler has a Session as state.
 *
 *
 * @.usage      To access a jms QueueReceiver for module context named REF in a
 *              <em>transacted mode</em> with logical queueId named in,clients should call:
 * <pre>
 *      QueueHandler handler = new QueueHandler(true);
 *      IQueueReceiver rcvr = handler.newReceiver(Module.REF, "IN");
 * </pre>
 *
 * @version     $Revision: 1.24 $, $Date: 2006/06/06 12:10:36 $
 *
 *
 * @since       1.0
 *
 */
public class QueueHandler {

    //~ Class Attributes =======================================================

    /**
     * The <code>Log</code> instance for this class.
     */
    private static final Log log = LogFactory.getLog(QueueHandler.class);

    /**
     * Default outq name.
     */
    private static final String DEFAULT_OUTQ_NAME = "outq";
    
    //~ Instance Attributes ====================================================

    /**
     * The transaction cover marker.
     */
    private boolean mTransacted;

    /**
     * The queue connection.
     */
    //private QueueConnection mQueueConn;

    /**
     * The queue session.
     */
    //private QueueSession mQueueSession;    
    
    private JmsConnectionFactory.JmsPoolObject m_poolObject;
    
    //~ Constructors ===========================================================
    /**
     * Constructs a QueueHandler.
     * 
     *
     * @param   transacted      the transaction cover marker 
     *
     *
     */
    public QueueHandler(boolean transacted) {
    
        mTransacted = transacted;
    }

    //~ Instance Methods =================================================
    /**
     * Aquisition of resource on first use basis. 
     * 
     *
     * @throws  xxxxxxxxException Indicating failure to open connection.
     * @throws  JMSException  Indicating failure to create session.
     *
     */
    private void lazyInit()
            throws xxxxxxxxException, 
            JMSException {
        if (m_poolObject == null) {
        	m_poolObject = JmsConnectionFactory.getInstance().openConnection(mTransacted);
        }

    }

    /**
     * Get the transactional mode of the QueueHandler. 
     * 
     * @return  boolean mode of the transactional cover.
     */
    public boolean getTransacted() {

        return mTransacted;
    }

    /**
     * Commits the action on the queue. 
     * 
     * Commits all messages done in this transaction and releases any locks
     * currently held.
     *
     * @throws  xxxxxxxxException  wrapping the original root cause
     * 
     */
    public void commit()
            throws xxxxxxxxException {

        try {
            if (m_poolObject != null) {
            	m_poolObject.getQueueSession().commit();
            }
        } catch (JMSException ex) {
            log.fatal(ex);
            throw new xxxxxxxxStandardException(ex);
        }

    }
    
    
    /**
     * Rollbacks the action on the queue. 
     * 
     * Rollbacks all messages done in this transaction and releases any locks 
     * currently held.
     *
     * @throws  xxxxxxxxException  wrapping the original root cause
     * 
     */
    public void rollback()
            throws xxxxxxxxException {

        try {
            if (m_poolObject != null) {
            	m_poolObject.getQueueSession().rollback();
            }
        } catch (JMSException ex) {
            log.fatal(ex);
            throw new xxxxxxxxStandardException(ex);
        }

    }
    
    
    /**
     * Closes the session.  
     * 
     * Since a provider may allocate some resources on behalf of a session outside 
     * the JVM, clients should close the resources when they are not needed. 
     * Relying on garbage collection to eventually reclaim these resources may not 
     * be timely enough. 
     *
     * 
     */
    public void close() {

        try {
            if (m_poolObject != null) {

                if (TxnManager.getInstance().isTransactionBound(this)) {

                    // FIXME
                    // Do not throw exception - rather log this in warning mode
                    // and delist connection from TM
                    // Go on closing connection

                    Exception e = new UnsupportedOperationException
                    ("Close a transaction bound queueHandler while transaction is still open. " +
                     "Resource will be delisted from transaction and transaction on this" +
                     " resource will be rolled back");
                    log.warn("Queuehandler close in TM",e);
                
                    TxnManager.getInstance().delistResource(this);
                }

                JmsConnectionFactory.getInstance().closeConnection(m_poolObject);
                m_poolObject = null;
            }
        } catch (Exception ex) {
            log.info(ex);
        }

    } 
    

    /**
     * Get a new IQueueReceiver. 
     * 
     * Messages are received using a IQueueReceiver.
     *
     * @param   module  the module from whose context the receiver will be returned
     * @param   logicalQueueId  the logical queue id    
     * @throws  xxxxxxxxException  wrapping the original root cause
     * 
     *
     * @return  IQueueReceiver for use
     */
    public IQueueReceiver newReceiver(Module module,
                                      String logicalQueueId)
            throws xxxxxxxxException {

        return newReceiver(module, logicalQueueId, StringUtils.EMPTY);

    } 

    /**
     * Get a new IQueueReceiver. 
     * 
     * Messages are received using a IQueueReceiver.
     *
     * @param   module  the module from whose context the receiver will be returned
     * @param   logicalQueueId  the logical queue id    
     * @param   messageSelector  only messages with properties matching the message 
     * selector expression are delivered. A value of null or an empty string indicates 
     * that there is no message selector for the message consumer.    
     * @throws  xxxxxxxxException  wrapping the original root cause
     * 
     *
     * @return  IQueueReceiver for use
     */
    public IQueueReceiver newReceiver(Module module,
                                      String logicalQueueId,
                                      String messageSelector)
            throws xxxxxxxxException {

        try {
            Queue queue = getPhysicalQueue(module, logicalQueueId);
            QueueReceiver rcvr = m_poolObject.getQueueSession().createReceiver(queue, messageSelector);
            XQueueReceiver xrcvr = new XQueueReceiver(rcvr);

            if (mTransacted && !TxnManager.getInstance().enlistResource(this)) {
                if (log.isDebugEnabled()) {
                    log.debug("QueueHandler not under 2-phase transaction cover");
                }
            }

            return xrcvr;

        } catch (JMSException e) {
            log.fatal(e);
            throw new xxxxxxxxStandardException(e);
        }

    } 

    /**
     * Get a new IQueueSender. 
     * 
     * Messages are sent using a IQueueSender.
     *
     * @param   module  the module from whose context the sender will be returned
     * @param   logicalQueueId  the logical queue id    
     * @throws  xxxxxxxxException  wrapping the original root cause 
     *
     * @return  IQueueSender for use
     */
    public IQueueSender newSender(Module module,
                                  String logicalQueueId)
            throws xxxxxxxxException {

        try {
            Queue queue = getPhysicalQueue(module, logicalQueueId);
            QueueSender sndr = m_poolObject.getQueueSession().createSender(queue);
            XQueueSender xsndr = new XQueueSender(sndr);

            if (mTransacted && !TxnManager.getInstance().enlistResource(this)) {
                if (log.isDebugEnabled()) {
                    log.debug("QueueHandler not under 2-phase transaction cover");
                }
            }

            return xsndr;

        } catch (JMSException e) {
            log.fatal(e);
            throw new xxxxxxxxStandardException(e);
        }

    } 
    
    /**
     * Get a new sender for sending message to the <code>current</code> output queue.
     * <p/>
     * Current output queue is dependent on context. For message processing,
     * the output queue is known beforehand whereas from screen it defaults to "outq".
     * This method allows callers to create a sender without requiring them to pass a logical
     * queue-id.
     * <p/>
     * The method resolves the logical queue id as follows:
     * <ul>
     * <li>First, {@link IModuleContext context associated with the passed module} is accessed.</li>
     * <li>Value of the named attribute {@link Globals#OUTQ_KEY} is queried using {@link IContext#getAttribute(String)}.</li>
     * <li>If a non-empty value is found, it is assumed to be the current logical queue-id.</li>
     * <li>Otherwise, a default output queue <code>outq</code> is assumed.</li>
     * <li>Call is delegated to {@link #newSender(Module, String)} method with the deduced queue id</li>
     * </ul>
     * 
     * @param module            module for which a sender is requested.
     * @return                  a sender instance, never null.
     * @throws xxxxxxxxException   on failure to access module context or if the sender cannot be created.
     */
    public IQueueSender newContextOutQSender(Module module)
        throws xxxxxxxxException {
        
        IModuleContext mc = Application.getInstance().
                                getContext().getModuleContext(module);
        String ctxOutQ = (String)mc.getAttribute(Globals.OUTQ_KEY);
        if(StringUtils.isEmpty(ctxOutQ))
            ctxOutQ = DEFAULT_OUTQ_NAME;
        
        return newSender(module,ctxOutQ);
    }

    /**
     * Get a new IQueueBrowser. 
     * 
     * A client uses a IQueueBrowser object to look at messages on a queue without 
     * removing them.
     *
     * @param   module  the module from whose context the browser will be returned
     * @param   logicalQueueId  the logical queue id    
     * @throws  xxxxxxxxException  wrapping the original root cause 
     *
     * @return  IQueueBrowser for use
     */
    public IQueueBrowser newBrowser(Module module,
                                    String logicalQueueId)
            throws xxxxxxxxException {

        return newBrowser(module, logicalQueueId, StringUtils.EMPTY);
    }

    /**
     * Get a new IQueueBrowser. 
     * 
     * A client uses a IQueueBrowser object to look at messages on a queue without 
     * removing them.
     *
     * @param   module  the module from whose context the browser will be returned
     * @param   logicalQueueId  the logical queue id 
     * @param   messageSelector  only messages with properties matching the message 
     * selector expression are delivered. A value of null or an empty string indicates 
     * that there is no message selector for the message consumer.    
     *
     * @throws  xxxxxxxxException  wrapping the original root cause 
     *
     * @return  IQueueBrowser for use
     */
    public IQueueBrowser newBrowser(Module module,
                                    String logicalQueueId,
                                    String messageSelector)
            throws xxxxxxxxException {

        try {
            Queue queue = getPhysicalQueue(module, logicalQueueId);
            QueueBrowser brsr = m_poolObject.getQueueSession().createBrowser(queue, messageSelector);
            XQueueBrowser xbrsr = new XQueueBrowser(brsr);
            return xbrsr;

        } catch (JMSException e) {
            log.fatal(e);
            throw new xxxxxxxxStandardException(e);
        }

    }

    /**
     * Get a new IQueueMessage with the default protocol. 
     * 
     * A client uses a IQueueMessage object to send a message containing a java.lang.String.
     *
     * @param   content  the message content        
     * @throws  xxxxxxxxException  wrapping the original root cause 
     *
     * @return  IQueueMessage for use
     */
    public IQueueMessage newQueueMessage(String content)
            throws xxxxxxxxException {

        IMessageProtocol protocol = Application.getInstance().getContext()
                                              .getMessageProtocol();
        IQueueMessage qm = newQueueMessage(content, protocol);
        return qm;

    }

    /**
     * Get a new IQueueMessage with the supplied protocol. 
     * 
     * A client uses a IQueueMessage object to send a message containing a java.lang.String.
     *
     * @param   content  the message content    
     * @param   protocol  the supplied protocol             
     * @throws  xxxxxxxxException  wrapping the original root cause 
     *
     * @return  IQueueMessage for use
     */
    public IQueueMessage newQueueMessage(String content,
                                         IMessageProtocol protocol)
            throws xxxxxxxxException {

        if (content == null) { throw new IllegalArgumentException(
                                                                  "message content is null"); }

        if (protocol == null) { throw new IllegalArgumentException(
                                                                   "message protocol is null"); }


        try {
            lazyInit();

            Message message = protocol.toMessage(content,getSession());
            return new XQueueMessage(message, content);

        } catch (JMSException e) {
            log.fatal(e);
            throw new xxxxxxxxStandardException(e);
        }

    }

    /**
     * Finalizes. 
     * 
     * The last defence.
     *
     * @throws  Throwable  wrapping the original root cause 
     *
     */
    public void finalize() throws Throwable {

        close();
        super.finalize();

    }

    /**
     * Get the queue session. 
     * Package private
     * 
     * @return QueueSession for use
     *
     */
    QueueSession getSession() {

        return m_poolObject.getQueueSession();
    }
     
    //~ Private Implementation Helper

    /**
     * Get physical queue.
     * @param   module  the supplied module          
     * @param   logicalQueueId  the logical queue id   
     * @throws  xxxxxxxxException  wrapping the original root cause 
     * @throws  JMSException  Indicating failure to create session      
     *
     * @return  javax.jms.Queue for use
     */
    private Queue getPhysicalQueue(Module module,
                                   String logicalQueueId)
            throws xxxxxxxxException, 
            JMSException {

        lazyInit();
        String physicalQueueId = JmsConnectionFactory.getInstance().
                                      getPhysicalQueueName(module,logicalQueueId);
        MQQueue queue = (MQQueue) m_poolObject.getQueueSession().createQueue(physicalQueueId);
        
        boolean bJmsClient = JmsConnectionFactory.getInstance().
                               isQueueJMSClient(module,logicalQueueId);       
        if (log.isTraceEnabled() && bJmsClient) {
            log.trace("Setting jmsClient of [" + physicalQueueId + "] to true");
        }
        

        queue.setTargetClient(bJmsClient 
                              ? JMSC.MQJMS_CLIENT_JMS_COMPLIANT 
                              : JMSC.MQJMS_CLIENT_NONJMS_MQ);
        queue.setPriority(JMSC.MQJMS_PRI_APP);
        queue.setPersistence(JMSC.MQJMS_PER_QDEF);
        return queue;

    }
}
