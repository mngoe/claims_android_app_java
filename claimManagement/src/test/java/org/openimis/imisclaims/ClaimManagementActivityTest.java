package org.openimis.imisclaims;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

<<<<<<< HEAD
public class ClaimManagementActivityTest {

    @Test
    public void addition_isCorrect() throws Exception {
        int expected = 5;
        int actual = (2 + 3); // pretend this is a static method in the Util class in your android project
        assertEquals(expected, actual);
=======
    /**
     * Creates a new ClaimManagementActivityTest object.
     */
    public ClaimManagementActivityTest ()
    {
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
>>>>>>> develop
    }
}