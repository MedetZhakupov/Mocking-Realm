package info.juanmendez.mockrealm.decorators;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.concurrent.Callable;

import info.juanmendez.mockrealm.dependencies.Compare;
import info.juanmendez.mockrealm.dependencies.RealmMatchers;
import info.juanmendez.mockrealm.dependencies.RealmStorage;
import info.juanmendez.mockrealm.dependencies.TransactionObservable;
import info.juanmendez.mockrealm.models.Query;
import info.juanmendez.mockrealm.models.TransactionEvent;
import info.juanmendez.mockrealm.utils.QueryHolder;
import info.juanmendez.mockrealm.utils.RealmModelUtil;
import io.realm.Realm;
import io.realm.RealmAsyncTask;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by @juanmendezinfo on 2/15/2017.
 */
public class RealmDecorator {

    private static Scheduler observerScheduler = Schedulers.immediate();
    private static Scheduler subscriberScheduler = Schedulers.immediate();

    public static Realm prepare() throws Exception {

        Realm realm = mock(Realm.class );
        prepare(realm);
        handleAsyncTransactions(realm);
        handleSyncTransactions(realm);
        return  realm;
    }

    public static void setTransactionScheduler(Scheduler observerScheduler) {
        RealmDecorator.observerScheduler = observerScheduler;
    }

    public static void setResponseScheduler(Scheduler subscriberScheduler) {
        RealmDecorator.subscriberScheduler = subscriberScheduler;
    }

    public static Scheduler getTransactionScheduler() {
        return observerScheduler;
    }

    public static Scheduler getResponseScheduler() {
        return subscriberScheduler;
    }

    private static void prepare(Realm realm) throws Exception {

        doNothing().when( Realm.class, "init", any());

        when( Realm.deleteRealm( any(RealmConfiguration.class))).thenReturn( true );

        HashMap<Class, RealmList<RealmModel>> realmMap = RealmStorage.getRealmMap();

        when(Realm.getDefaultInstance()).thenReturn(realm);

        when( realm.createObject( Mockito.argThat(new RealmMatchers.ClassMatcher<>(RealmModel.class)) ) ).thenAnswer(invocation -> {
            Class clazz = (Class) invocation.getArguments()[0];

            if( !realmMap.containsKey(clazz)){
                realmMap.put(clazz, RealmListDecorator.create());
            }

            RealmModel realmModel = RealmModelDecorator.create(clazz, true);
            RealmStorage.addModel( realmModel );

            return realmModel;
        });

        when( realm.copyToRealm(Mockito.any( RealmModel.class ))).thenAnswer( new Answer<RealmModel>(){

            @Override
            public RealmModel answer(InvocationOnMock invocationOnMock) throws Throwable {

                if( invocationOnMock.getArguments().length > 0 ){
                    RealmModel realmModel = (RealmModel) invocationOnMock.getArguments()[0];

                    if( realmModel instanceof RealmObject ){
                        realmModel = RealmModelDecorator.mockRealmObject( (RealmObject) realmModel );
                    }

                    Class clazz = RealmModelUtil.getClass(realmModel);
                    HashMap<Class, RealmList<RealmModel>> realmMap = RealmStorage.getRealmMap();

                    if( !realmMap.containsKey(clazz)){
                        realmMap.put(clazz, RealmListDecorator.create());
                    }


                    RealmStorage.addModel( realmModel );
                    return realmModel;
                }

                return null;
            }
        });

        when( realm.where( Mockito.argThat( new RealmMatchers.ClassMatcher<>(RealmModel.class))  ) ).then(new Answer<RealmQuery>(){


            @Override
            public RealmQuery answer(InvocationOnMock invocationOnMock) throws Throwable {

                //clear list being queried
                Class clazz = (Class) invocationOnMock.getArguments()[0];
                QueryHolder queryHolder = new QueryHolder(clazz);

                RealmQuery realmQuery = RealmQueryDecorator.create(queryHolder);

                if( !realmMap.containsKey(clazz))
                {
                    realmMap.put(clazz, new RealmList<>());
                }

                queryHolder.appendQuery( new Query(Compare.startTopGroup, new Object[]{realmMap.get(clazz)} ));


                return realmQuery;
            }
        });
    }

    private static void handleAsyncTransactions(Realm realm ){

        //call execute() in Realm.Transaction object received.
        doAnswer( new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                if( invocation.getArguments().length > 0 ){
                    Realm.Transaction transaction = (Realm.Transaction) invocation.getArguments()[0];

                    queueTransaction(() -> {
                        transaction.execute( realm );
                        return null;
                    });
                }
                return null;
            }
        }).when( realm ).executeTransaction(any( Realm.Transaction.class ));

        doAnswer( new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                if( invocation.getArguments().length > 0 ){

                    queueTransaction( () -> {
                        Observable.fromCallable(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                Realm.Transaction transaction = (Realm.Transaction) invocation.getArguments()[0];
                                transaction.execute( realm );
                                return null;
                            }
                        })
                        .subscribeOn(observerScheduler)
                        .observeOn( subscriberScheduler ).subscribe(aVoid -> {});

                        return  null;
                    });

                }

                return null;
            }
        }).when( realm ).executeTransactionAsync(any( Realm.Transaction.class ));


        when( realm.executeTransactionAsync(any( Realm.Transaction.class ), any( Realm.Transaction.OnSuccess.class ))  ).thenAnswer(
                new Answer<RealmAsyncTask>() {

                    @Override
                    public RealmAsyncTask answer(InvocationOnMock invocation) throws Throwable {

                        Realm.Transaction transaction = (Realm.Transaction) invocation.getArguments()[0];

                        queueTransaction(() -> {

                            Observable.fromCallable(new Callable<Boolean>() {
                                @Override
                                public Boolean call() throws Exception {
                                    if( invocation.getArguments().length >=1 ){
                                        transaction.execute( realm );
                                        return true;
                                    }
                                    return false;
                                }
                            })
                            .subscribeOn(observerScheduler)
                            .observeOn( subscriberScheduler )
                            .subscribe(aBoolean -> {
                                if(  aBoolean && invocation.getArguments().length >=2 ){
                                    Realm.Transaction.OnSuccess onSuccess = (Realm.Transaction.OnSuccess) invocation.getArguments()[1];
                                    onSuccess.onSuccess();
                                }
                           });

                            return null;
                        });

                        //TODO: fix this in future phase
                        return new RealmAsyncTask() {
                            @Override
                            public void cancel() {

                            }

                            @Override
                            public boolean isCancelled() {
                                return false;
                            }
                        };
                    }
                }
        );


        when( realm.executeTransactionAsync(any( Realm.Transaction.class ), any( Realm.Transaction.OnSuccess.class ), any(Realm.Transaction.OnError.class))  ).thenAnswer(
                new Answer<RealmAsyncTask>() {

                    @Override
                    public RealmAsyncTask answer(InvocationOnMock invocation) throws Throwable {

                        queueTransaction(() -> {
                            Observable.fromCallable(new Callable<Boolean>() {
                                @Override
                                public Boolean call() throws Exception {
                                    if( invocation.getArguments().length >=1 ){
                                        Realm.Transaction transaction = (Realm.Transaction) invocation.getArguments()[0];
                                        transaction.execute(realm);
                                        return true;
                                    }

                                    return false;
                                }
                            })
                            .subscribeOn(observerScheduler)
                            .observeOn( subscriberScheduler )
                            .subscribe(aBoolean -> {
                                if(  aBoolean && invocation.getArguments().length >=2 ){
                                    Realm.Transaction.OnSuccess onSuccess = (Realm.Transaction.OnSuccess) invocation.getArguments()[1];
                                    onSuccess.onSuccess();
                                }

                            }, throwable -> {

                                if( invocation.getArguments().length >=3 ){
                                    Realm.Transaction.OnError onError = (Realm.Transaction.OnError) invocation.getArguments()[2];
                                    onError.onError(throwable);
                                }

                            });

                            return null;
                        });



                        //TODO: fix this in future phase
                        return new RealmAsyncTask() {
                            @Override
                            public void cancel() {

                            }

                            @Override
                            public boolean isCancelled() {
                                return false;
                            }
                        };
                    }
                }
        );


        when( realm.executeTransactionAsync(any( Realm.Transaction.class ), any(Realm.Transaction.OnError.class))  ).thenAnswer(
                new Answer<RealmAsyncTask>() {

                    @Override
                    public RealmAsyncTask answer(InvocationOnMock invocation) throws Throwable {
                        Realm.Transaction transaction = (Realm.Transaction) invocation.getArguments()[0];

                        queueTransaction(() -> {

                            Observable.fromCallable(new Callable<Boolean>() {
                                @Override
                                public Boolean call() throws Exception {
                                    if( invocation.getArguments().length >=1 ){


                                        return true;
                                    }

                                    return false;
                                }
                            })
                            .subscribeOn(observerScheduler)
                            .observeOn( subscriberScheduler )
                            .subscribe(aBoolean -> {
                                if(  aBoolean && invocation.getArguments().length >=2 ){
                                    Realm.Transaction.OnSuccess onSuccess = (Realm.Transaction.OnSuccess) invocation.getArguments()[1];
                                    onSuccess.onSuccess();
                                }

                            }, throwable -> {

                                if( invocation.getArguments().length >=2 ){
                                    Realm.Transaction.OnError onError = (Realm.Transaction.OnError) invocation.getArguments()[2];
                                    onError.onError(throwable);
                                }

                            });

                            return null;
                        });

                        //TODO: fix this in future phase
                        return new RealmAsyncTask() {
                            @Override
                            public void cancel() {

                            }

                            @Override
                            public boolean isCancelled() {
                                return false;
                            }
                        };
                    }
                }
        );
    }

    private static void handleSyncTransactions(Realm realm ){

        TransactionObservable.KeyTransaction transaction = new TransactionObservable.KeyTransaction( realm.toString() );

        doAnswer(invocation -> {
            TransactionObservable.startRequest(transaction);
            return null;
        }).when( realm ).beginTransaction();


        doAnswer(invocation -> {
            TransactionObservable.endRequest(transaction);
            return null;
        }).when( realm ).commitTransaction();
    }

    private static void queueTransaction(Func0 funk){

        TransactionObservable.startRequest(funk,
                TransactionObservable.asObservable()
                        .filter(transactionEvent -> {
                            return transactionEvent.getState()== TransactionEvent.START_TRANSACTION && transactionEvent.getInitiator() == funk;
                    })
                    .subscribe(o -> {
                        funk.call();
                        TransactionObservable.endRequest(funk);
                    })
        );
    }
}