package com.example.beechats.data.repositories;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beechats.utils.AppLifecycleObserver;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests cho AppLifecycleObserver.
 * Mock FirebaseAuth + UserRepository để kiểm tra logic gọi updateOnlineStatus.
 */
public class AppLifecycleObserverTest {

    @Mock
    private FirebaseAuth mockAuth;

    @Mock
    private FirebaseUser mockFirebaseUser;

    @Mock
    private UserRepository mockUserRepository;

    private AppLifecycleObserver observer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockFirebaseUser.getUid()).thenReturn("uid-observer-test");
        observer = new AppLifecycleObserver(mockAuth, mockUserRepository);
    }

    // -----------------------------------------------------------------------
    // TC51: onStart() — user đang đăng nhập → updateOnlineStatus(uid, true) được gọi
    // -----------------------------------------------------------------------

    @Test
    public void onStart_userLoggedIn_callsUpdateOnlineStatusTrue() {
        when(mockAuth.getCurrentUser()).thenReturn(mockFirebaseUser);

        observer.onStart(null);

        verify(mockUserRepository).updateOnlineStatus(
                eq("uid-observer-test"),
                eq(true),
                org.mockito.ArgumentMatchers.any(UserRepository.OnCompleteCallback.class)
        );
    }

    // -----------------------------------------------------------------------
    // TC52: onStop() — user đang đăng nhập → updateOnlineStatus(uid, false) được gọi
    // -----------------------------------------------------------------------

    @Test
    public void onStop_userLoggedIn_callsUpdateOnlineStatusFalse() {
        when(mockAuth.getCurrentUser()).thenReturn(mockFirebaseUser);

        observer.onStop(null);

        verify(mockUserRepository).updateOnlineStatus(
                eq("uid-observer-test"),
                eq(false),
                org.mockito.ArgumentMatchers.any(UserRepository.OnCompleteCallback.class)
        );
    }

    // -----------------------------------------------------------------------
    // TC53: onStart() — user chưa đăng nhập (null) → updateOnlineStatus KHÔNG gọi
    // -----------------------------------------------------------------------

    @Test
    public void onStart_userNotLoggedIn_doesNotCallUpdateOnlineStatus() {
        when(mockAuth.getCurrentUser()).thenReturn(null);

        observer.onStart(null);

        verify(mockUserRepository, never()).updateOnlineStatus(
                anyString(), anyBoolean(),
                org.mockito.ArgumentMatchers.any(UserRepository.OnCompleteCallback.class)
        );
    }
}
