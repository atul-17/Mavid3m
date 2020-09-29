package com.mavid.BLEApproach;

import java.io.IOException;

public interface BleReadInterface {

    public void onReadSuccess(byte[] data) throws IOException;
}
