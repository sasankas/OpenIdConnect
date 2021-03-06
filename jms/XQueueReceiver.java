

package com.nri.xxxxxxxx.inf;


import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueReceiver;

import com.nri.xxxxxxxx.inf.exception.xxxxxxxxException;
import com.nri.xxxxxxxx.inf.spi.*;
import com.nri.xxxxxxxx.startup.Application;


/**
 * Implementation for IQueueReceiver.
 *
 * @version     $Revision: 1.6 $, $Date: 2004/07/09 12:50:13 $
 *
 *
 * @since       1.0
 *
 */
public class XQueueReceiver
        implements IQueueReceiver {

    //~ Instance Attributes ====================================================

    /**
     * The wrapped QueueReceiver.
     */
    private QueueReceiver mQueueReceiver;


    //~ Constructors ===========================================================

    /**
     * Constructs a XQueueReceiver.
     * 
     * @param   receiver  the original javax.jms.QueueReceiver 
     *
     */
    XQueueReceiver(QueueReceiver receiver) {

        mQueueReceiver = receiver;
    }


    //~ Instance Methods =================================================

    /**
     * {@inheritDoc}.
     */
    public void close()
            throws JMSException {

        mQueueReceiver.close();
    }


    /**
     * {@inheritDoc}.
     */
    public IQueueMessage receive()
            throws JMSException {

        return receive(getDefaultProtocol());
    }


    /**
     * {@inheritDoc}.
     */
    public IQueueMessage receive(long timeout)
            throws JMSException {

        return receive(timeout, getDefaultProtocol());
    }


    /**
     * {@inheritDoc}.
     */
    public IQueueMessage receiveNoWait()
            throws JMSException {

        return receiveNoWait(getDefaultProtocol());
    }


    /**
     * {@inheritDoc}.
     */
    public IQueueMessage receive(IMessageProtocol protocol)
            throws JMSException {

        if (protocol == null) { throw new IllegalArgumentException(
                                                                   "message protocol is null"); }

        Message message = mQueueReceiver.receive();
        String content = protocol.toString(message);

        return new XQueueMessage(message, content);
    }


    /**
     * {@inheritDoc}.
     */
    public IQueueMessage receive(long timeout,
                                 IMessageProtocol protocol)
            throws JMSException {

        if (protocol == null) { throw new IllegalArgumentException(
                                                                   "message protocol is null"); }

        IQueueMessage qm = null;
        Message message = mQueueReceiver.receive(timeout);
        if (message != null) {
            String content = protocol.toString(message);
            qm = new XQueueMessage(message, content);
        }

        return qm;

    }


    /**
     * {@inheritDoc}.
     */
    public IQueueMessage receiveNoWait(IMessageProtocol protocol)
            throws JMSException {

        if (protocol == null) { throw new IllegalArgumentException(
                                                                   "message protocol is null"); }

        IQueueMessage qm = null;
        Message message = mQueueReceiver.receiveNoWait();
        if (message != null) {
            String content = protocol.toString(message);
            qm = new XQueueMessage(message, content);
        }

        return qm;

    }


    /**
     * {@inheritDoc}.
     */
    public String getMessageSelector()
            throws JMSException {

        return mQueueReceiver.getMessageSelector();
    }


    //~ Private Implementation Helper

    /**
     * Gets the default protocol.
     * @throws  JMSException  Indicating failure to get the protocol.
     *
     * @return  IMessageProtocol for use
     */
    private IMessageProtocol getDefaultProtocol()
            throws JMSException {

        try {
            return Application.getInstance().getContext().getMessageProtocol();
        } catch (xxxxxxxxException xe) {

            JMSException je = new JMSException("error obtaining protocol");
            je.setLinkedException(xe);
            throw je;
        }
    }
}

