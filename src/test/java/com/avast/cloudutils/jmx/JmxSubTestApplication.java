package com.avast.cloudutils.jmx;

import java.util.Scanner;

/**
 * Created by jacob on 6/25/15.
 */
public class JmxSubTestApplication extends JmxTestApplication {
    public JmxSubTestApplication() {
        super();
    }

    public static void main(String[] args) {
        new JmxSubTestApplication();
        new Scanner(System.in).nextLine();

    }
}
