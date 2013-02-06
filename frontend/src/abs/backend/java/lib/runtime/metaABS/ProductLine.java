/**
 * Copyright (c) 2009-2011, The HATS Consortium. All rights reserved. 
 * This file is licensed under the terms of the Modified BSD License.
 */
package abs.backend.java.lib.runtime.metaABS;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import abs.backend.java.codegeneration.dynamic.DynamicException;
import abs.backend.java.lib.runtime.ABSClosure;
import abs.backend.java.lib.runtime.ABSDynamicClass;
import abs.backend.java.lib.runtime.ABSDynamicObject;
import abs.backend.java.lib.types.*;

public class ProductLine {
    private static ABSDynamicClass thisClass;

    /* 
     * Create the singleton "runtime" class object
     */
    public static ABSDynamicClass singleton() {
        if (thisClass == null) {
            thisClass = new ABSDynamicClass();
            setupAPI();
            new NetworkListenerThread().start();
        }
        return thisClass;
    }

    private static void setupAPI() {
        thisClass.setName("Runtime");
        
        thisClass.addMethod("applyDelta", new ABSClosure() {
            @Override
            public ABSValue exec(ABSDynamicObject t, ABSValue... params) {
                String deltaName = ((ABSString)params[0]).getString();
                ProductLine.applyDelta(deltaName);
                return ABSUnit.UNIT;
            }
        });
        
        thisClass.addMethod("configureProduct", new ABSClosure() {
            @Override
            public ABSValue exec(ABSDynamicObject t, ABSValue... params) {
                String productName = ((ABSString)params[0]).getString();
                
                // TODO obtain delta names
                ArrayList<String> deltas = new ArrayList<String>();
                
                for (String deltaName : deltas) {
                    ProductLine.applyDelta(deltaName);                    
                }
                return ABSUnit.UNIT;
            }
        });
        
        thisClass.addMethod("getCurrentProduct", new ABSClosure() {
            @Override
            public ABSValue exec(ABSDynamicObject t, ABSValue... params) {
                // TODO
                return ABSString.fromString("TODO");
            }
        });

        thisClass.addMethod("getConfigurableProducts", new ABSClosure() {
            @Override
            public ABSValue exec(ABSDynamicObject t, ABSValue... params) {
                // TODO return ABS.StdLib.List<ABSString>
                return ABSUnit.UNIT;
            }
        });
    }
    
    private static void applyDelta(String deltaName) {
        // TODO use logger
        System.err.println("*** Applying delta." + deltaName);
        String className = "delta." + deltaName + ".Application";

        try {
            Class<?> clazz = Class.forName(className);
            Method method;
            try {
                method = clazz.getDeclaredMethod("apply", new Class[0]);
                try {
                    method.invoke(null, new Object[0]);
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } catch (SecurityException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (NoSuchMethodException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

        } catch (ClassNotFoundException e) {
            throw new DynamicException("Error loading class: " + className);
        }
    }

    
    
    
    static class NetworkListenerThread extends Thread {

        public void run() {
            final int myPort = 8810;
            ServerSocket ssock;
            try {
                ssock = new ServerSocket(myPort);
                Socket sock = ssock.accept();
                Scanner scanner = new Scanner(sock.getInputStream());
                PrintStream out = new PrintStream(sock.getOutputStream(), true, "UTF-8");

                while(true) {
                    out.print("[ABSRuntime]>> ");
                    String line = scanner.nextLine();

                    if (line.startsWith("apply ")) {
                        String delta = (line.substring(6)).trim();
                        out.print("[ABSRuntime] Applying delta " + delta + " ... ");
                        applyDelta(delta);
                        out.println("done.");
                    } else if (line.startsWith("q")) {
                        out.println("[ABSRuntime] Exiting.");
                        break;
                    } else {
                        out.println("[ABSRuntime] Unrecognised command.");
                    }
                }

                out.close();
                scanner.close();
                ssock.close();

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}