package com.mavid.libresdk.TaskManager.Communication.Listeners;

import com.mavid.libresdk.TaskManager.Discovery.Listeners.ListenerUtils.MessageInfo;

/**
 * Created by bhargav on 9/2/18.
 */

public interface CommandStatusListenerWithResponse extends CommandStatusListener {
    public void response(MessageInfo messageInfo);
}
