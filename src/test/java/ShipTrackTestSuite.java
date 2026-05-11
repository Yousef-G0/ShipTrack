

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        SecurityUtilsTest.class,
        ShipTrackSystemTest.class
})
public class ShipTrackTestSuite {
}