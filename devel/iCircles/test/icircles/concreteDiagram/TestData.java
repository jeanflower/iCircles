package icircles.concreteDiagram;

import icircles.test.TestDatum;

class TestData {
    /*
     * We use JSON objects as our input format.  These are verbose, but easy to
     * both understand and parse.  In the case of tests, we allow JSON objects
     * to have single quoted strings i.e. 'string'.  This is in violation of the
     * JSON standard, however it makes packing the JSON objects into Java
     * strings much more readable as we don't have to do "\"string\"".
     */
    public static TestDatum[] test_data = {
        new TestDatum ("{'AbstractDiagram' : " +
                        "{'Version'     : 0," +
                        " 'Contours'    : ['a']," +
                        " 'Zones'       : [{'in' : []},{'in' : ['a']}]," +
                        " 'ShadedZones' : []," +
                        " 'Spiders'     : []" +
                        "}" +
                       "}", 80.35747263647977)
    };
}