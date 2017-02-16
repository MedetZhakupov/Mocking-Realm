import info.juanmendez.learn.realms.models.Dog;
import info.juanmendez.mock.realm.MockRealm;
import info.juanmendez.mock.realm.factories.RealmFactory;
import io.realm.*;
import io.realm.internal.RealmCore;
import io.realm.log.RealmLog;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by @juanmendezinfo on 2/10/2017.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.mockito.*", "android.*"})
@PrepareForTest({Realm.class, RealmConfiguration.class, RealmQuery.class, RealmResults.class, RealmCore.class, RealmLog.class, RealmAsyncTask.class, Realm.Transaction.class })
public class RealmMock
{
    Realm realm;

    @Before
    public void before() throws Exception {
        MockRealm.prepare();
        realm = RealmFactory.create();
    }
    @Test
    public void checkIfDefaultIsOurRealm(){
        assertEquals("is the same?", realm, Realm.getDefaultInstance());
    }

    @Test
    public void testCreateObject(){
        assertNotNull( realm.createObject(Dog.class));
    }

    /**
     * Realm mocked is suppose to bounce back same object to copyToRealm
     * @throws Exception
     */
    @Test
    public void testCopyToRealm() throws Exception {

        Dog dog = new Dog();
        dog.setName("Max");
        dog.setAge(1);

        assertEquals("is same dog?", dog, realm.copyToRealm( dog ) );
    }

    /**
     * So lets see if I can create an object, and I can get a realmResult back, and check the size to be equal to 1
     */
    @Test
    public void testExecuteTransaction(){
        realm.executeTransaction( realm1 -> {
            Dog dog = realm.createObject(Dog.class);
            dog.setAge(1);
            dog.setName("Max");
            dog.setBirthdate( new Date(2011, 6, 10));
        });

        assertEquals( "there is now one element available", realm.where(Dog.class).findAll().size(), 1 );
    }

    /**
     * assures we can get back dogs who were born after 2009.
     */
    @Test
    public void testConditions(){
        Dog dog = realm.createObject(Dog.class);
        dog.setAge(1);
        dog.setName("Max");
        dog.setBirthdate( new Date(2011, 6, 10));

        dog = realm.createObject(Dog.class);
        dog.setAge(2);
        dog.setName("Rex");
        dog.setBirthdate( new Date(2016, 6, 10));


        RealmResults<Dog> dogs = realm.where(Dog.class).greaterThanOrEqualTo("birthdate", new Date(2009, 6, 10) ).findAll();
        assertNotNull( "dog is found", dogs  );

        //iteration is working
        for( Dog _dog: dogs ){
            System.out.println( "dog: " + _dog.getName() );
        }
    }

    @Test
    public void testSynchronousTransaction(){
        realm.executeTransaction(realm1 -> {
            Dog dog = realm.createObject(Dog.class);
            dog.setAge(1);
            dog.setName("Max");
            dog.setBirthdate( new Date(2011, 6, 10));
        });

        assertEquals( "Synchronous added first item", realm.where(Dog.class).findFirst().getName(), "Max" );
    }

    @Test
    public void testAsyncTransactionOnSuccessAndError(){

        realm.executeTransactionAsync( realm1 -> {
            Dog dog = realm.createObject(Dog.class);
            dog.setAge(1);
            dog.setName("Max");
            dog.setBirthdate( new Date(2011, 6, 10));
        }, () ->{
            System.out.println( "this dog made was succesfully saved!");
        });

        realm.executeTransactionAsync( realm1 -> {
            Dog dog = realm.createObject(Dog.class);
            dog.setAge(1);
            dog.setName("Max");
            dog.setBirthdate( new Date(2011, 6, 10));
        }, () ->{
            System.out.println( "this dog made was succesfully saved!");
        }, error -> {
        });

        assertEquals( "There are two items found after async transactions", realm.where(Dog.class).findAll().size(), 2 );

        realm.executeTransactionAsync( realm1 -> {
            throw new  RuntimeException("Making a big deal because there are no more dogs to add" );
        }, () ->{
            System.out.println( "this dog made was succesfully saved!");
        }, error -> {
            System.err.println( "Transaction.error: " + error.getMessage() );
        });
    }
}