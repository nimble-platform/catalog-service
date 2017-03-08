package eu.nimble.registration_service;

import eu.nimble.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.FileUtility;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.JAXBUtility;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by suat on 23-Feb-17.
 */
public class RegistrationServiceImplTest {
    private static org.h2.tools.Server server = null;

    @BeforeClass
    public static void startH2DB() {
	    HibernateUtility.startH2DB();
    }

    @AfterClass
    public static void stopH2DB() {
	    HibernateUtility.stopH2DB();
    }

    @Test
    public void addUserTest() {
        String personXML = FileUtility.readFile("../ubl-data-model/src/test/resources/UBL-PersonFullDummy.xml");
        PersonType user = (PersonType) JAXBUtility.deserialize(personXML, Configuration.UBL_CAC_NS);
        long userId = user.getHjid();
        RegistrationServiceImpl.getInstance().addUser(user);
        user = (PersonType) HibernateUtility.getInstance().load(PersonType.class, user.getHjid());

        Assert.assertEquals("User ids do not match", (long) user.getHjid(), userId);
    }
}
