package org.openimis.imisclaims;

import junit.framework.TestCase;

public class ClaimManagementActivityTest extends TestCase {

    /**
     * Creates a new ClaimManagementActivityTest object.
     */
    public ClaimManagementActivityTest ()
    {
    }

    /**
     * Creates a new ClaimManagementActivityTest object.
     *
     * @param name DOCUMENT ME!
     */
    public ClaimManagementActivityTest (String name)
    {
        super(name);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // assertNears //
    //-------------//
    public static void assertNears (String msg,
                                    double a,
                                    double b)
    {
        assertNears(msg, a, b, 1E-5);
    }

    //-------------//
    // assertNears //
    //-------------//
    public static void assertNears (String msg,
                                    double a,
                                    double b,
                                    double maxDiff)
    {
        System.out.println("Comparing " + a + " and " + b);
        assertTrue(msg, Math.abs(a - b) < maxDiff);
    }

    //----------------//
    // checkException //
    //----------------//
    public static void checkException (Exception ex)
    {
        System.out.println("Got " + ex);
        assertNotNull(ex.getMessage());
    }

    //-------//
    // print //
    //-------//
    public static void print (Object o)
    {
        System.out.println(o);
    }

//    public void testDummy ()
//    {
//        // Nothing
//    }
//

    public void testDummy ()
    {
        // Nothing
    }

    //---------//
    // runTest //
    //---------//

    @Override
    protected void runTest ()
            throws Throwable
    {
        System.out.println("\n---\n" + getName() + ":");
        super.runTest();
        System.out.println("+++ End " + toString());
    }
}