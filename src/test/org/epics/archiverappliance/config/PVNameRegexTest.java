package org.epics.archiverappliance.config;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Small unit test to make sure the regex that checks for valid pv names does something reasonable
 * @author mshankar
 *
 */
public class PVNameRegexTest {

    @Test
    public void testValidPVNames() {

        List<String> pvNames = List.of(
                "archappl:arch:sine",
                "archappl:arch:sine1",
                "archappl:arch:sine1.HIHI",
                "archappl:arch:sine100",
                "archappl_arch_sine",
                "archappl-arch-sine",
                "archappl+arch+sine",
                "archappl[arch]sine",
                "archappl<arch>sine",
                "archappl:arch;sine",
                "archappl:ar/h:sine",
                "P#6:000357",
                "LN-AM{RadMon:1}Lvl:Raw-I",
                "TBGR001E01.{'dbnd':{'abs':1}}",
                "archappl:ar'h:sine",
                "CrS-PHS:Cryo-D-0100:AmbientTemp.{'dbnd':{'rel':0.1}}");
        for (String pvName : pvNames) {
            assertTrue("Valid pvName is deemed invalid " + pvName, PVNames.isValidPVName(pvName));
            assertEquals(pvName, PVNames.normalizePVName(pvName));
        }
    }

    @Test
    public void testNormalizePVName() {
        String pvName = "CrS-PHS:Cryo-D-0100:AmbientTemp.VAL";
        assertEquals("CrS-PHS:Cryo-D-0100:AmbientTemp", PVNames.normalizePVName(pvName));
    }

    @Test
    public void testInvalidPVNames() {
        List<String> pvNames = List.of(
                "\"archappl:arch:sine",
                "archappl:arch:sine\"",
                "archappl:ar!h:sine",
                "archappl:ar$h:sine",
                "archappl:ar%h:sine",
                "archappl:ar*h:sine",
                "archappl:ar\\h:sine",
                "archappl:ar|h:sine",
                "archappl:ar?h:sine",
                "archappl:ar&h:sine",
                "archappl:ar@ch:sine",
                "archappl:(arch):sine",
                "archappl:arch:sine=1.0",
                "");
        for (String pvName : pvNames) {
            assertFalse("Invalid pvName is deemed valid " + pvName, PVNames.isValidPVName(pvName));
        }
        assertFalse("null is deemed invalid", PVNames.isValidPVName(null));
    }
}
