package info.juanmendez.mock.realm.factories;

import info.juanmendez.mock.realm.dependencies.RealmStorage;
import io.realm.RealmObject;
import io.realm.RealmResults;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by @juanmendezinfo on 2/15/2017.
 */
public class ResultsFactory {

    public static RealmResults create( Class clazz ){

        ArrayList<RealmObject> realResults = RealmStorage.getQueryMap().get(clazz);
        RealmResults realmResults = PowerMockito.mock( RealmResults.class );

        when( realmResults.get(anyInt())).thenAnswer(positionInvokation -> {
            int position = (int) positionInvokation.getArguments()[0];
            return realResults.get( position );
        });

        when( realmResults.size() ).thenReturn( realResults.size() );
        when( realmResults.iterator() ).thenReturn( realResults.iterator() );
        when( realmResults.set(anyInt(), any(RealmObject.class)) ).thenAnswer(new Answer<RealmObject>() {
            @Override
            public RealmObject answer(InvocationOnMock invocationOnMock) throws Throwable {
                int index = (int) invocationOnMock.getArguments()[0];
                RealmObject value = (RealmObject) invocationOnMock.getArguments()[0];
                realResults.set(index, value);
                return value;
            }
        });

        when( realmResults.listIterator() ).thenReturn( realResults.listIterator() );

        when( realmResults.listIterator(anyInt()) ).thenAnswer(new Answer<RealmObject>() {
            @Override
            public RealmObject answer(InvocationOnMock invocationOnMock) throws Throwable {
                int index = (int) invocationOnMock.getArguments()[0];
                return realResults.get(index);
            }
        });

        return realmResults;
    }
}