package com.example.beechats.data.repositories;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beechats.data.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests cho FirebaseAuthRepository.register().
 * Mock Firebase Auth và UserRepository để test không cần device/emulator.
 */
public class FirebaseAuthRepositoryTest {

    @Mock
    private FirebaseAuth mockAuth;

    @Mock
    private UserRepository mockUserRepository;

    @Mock
    private FirebaseAuthRepository.OnAuthCallback mockCallback;

    private FirebaseAuthRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new FirebaseAuthRepository(mockAuth, mockUserRepository);
    }

    // -----------------------------------------------------------------------
    // TC1: Happy Path — Auth thành công + Firestore thành công → onSuccess()
    // -----------------------------------------------------------------------

    @Test
    public void register_validInput_callsOnSuccess() {
        // Mock Firebase Auth trả về thành công
        FirebaseUser mockUser = mock(FirebaseUser.class);
        when(mockUser.getUid()).thenReturn("test-uid-123");

        AuthResult mockAuthResult = mock(AuthResult.class);
        when(mockAuthResult.getUser()).thenReturn(mockUser);

        Task<AuthResult> mockTask = buildSuccessTask(mockAuthResult);
        when(mockAuth.createUserWithEmailAndPassword(eq("test@example.com"), eq("password123")))
                .thenReturn(mockTask);

        // Mock UserRepository.createUser() trả về thành công ngay lập tức
        doAnswer(inv -> {
            UserRepository.OnCompleteCallback cb = inv.getArgument(1);
            cb.onSuccess();
            return null;
        }).when(mockUserRepository).createUser(any(User.class), any(UserRepository.OnCompleteCallback.class));

        // Thực thi
        repository.register("test@example.com", "password123", "Nguyễn Văn A", mockCallback);

        // Kiểm tra: onSuccess() phải được gọi đúng 1 lần, onError() không được gọi
        verify(mockCallback).onSuccess();
        verify(mockCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC2: Error Case — Email đã tồn tại → onError("Email đã được sử dụng.")
    // -----------------------------------------------------------------------

    @Test
    public void register_duplicateEmail_callsOnErrorWithVietnameseMessage() {
        // Giả lập FirebaseAuthUserCollisionException
        FirebaseAuthUserCollisionException exception =
                mock(FirebaseAuthUserCollisionException.class);

        Task<AuthResult> mockTask = buildFailureTask(exception);
        when(mockAuth.createUserWithEmailAndPassword(anyString(), anyString()))
                .thenReturn(mockTask);

        repository.register("duplicate@example.com", "password123", "Test User", mockCallback);

        // onError phải nhận đúng thông báo tiếng Việt
        verify(mockCallback).onError("Email đã được sử dụng.");
        verify(mockCallback, never()).onSuccess();
        // Firestore không được gọi khi Auth thất bại
        verify(mockUserRepository, never()).createUser(any(), any());
    }

    // -----------------------------------------------------------------------
    // TC3: Error Case — Mật khẩu yếu → onError(chứa "6 ký tự")
    // -----------------------------------------------------------------------

    @Test
    public void register_weakPassword_callsOnErrorWithPasswordMessage() {
        // Giả lập FirebaseAuthWeakPasswordException
        FirebaseAuthWeakPasswordException exception =
                mock(FirebaseAuthWeakPasswordException.class);

        Task<AuthResult> mockTask = buildFailureTask(exception);
        when(mockAuth.createUserWithEmailAndPassword(anyString(), anyString()))
                .thenReturn(mockTask);

        repository.register("test@example.com", "123", "Test User", mockCallback);

        // Kiểm tra thông báo chứa nội dung về 6 ký tự
        verify(mockCallback).onError("Mật khẩu phải có ít nhất 6 ký tự.");
        verify(mockCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC4: Validate Input — Email rỗng → onError() ngay, Firebase không được gọi
    // -----------------------------------------------------------------------

    @Test
    public void register_emptyEmail_callsOnErrorWithoutCallingFirebase() {
        repository.register("", "password123", "Test User", mockCallback);

        verify(mockCallback).onError("Email không được để trống.");
        verify(mockCallback, never()).onSuccess();
        // Firebase Auth không được gọi
        verify(mockAuth, never()).createUserWithEmailAndPassword(anyString(), anyString());
    }

    @Test
    public void register_emptyPassword_callsOnError() {
        repository.register("test@example.com", "", "Test User", mockCallback);

        verify(mockCallback).onError("Mật khẩu không được để trống.");
        verify(mockAuth, never()).createUserWithEmailAndPassword(anyString(), anyString());
    }

    @Test
    public void register_emptyDisplayName_callsOnError() {
        repository.register("test@example.com", "password123", "  ", mockCallback);

        verify(mockCallback).onError("Tên hiển thị không được để trống.");
        verify(mockAuth, never()).createUserWithEmailAndPassword(anyString(), anyString());
    }

    // -----------------------------------------------------------------------
    // TC7: Login — Happy Path: Auth thành công → updateOnlineStatus(uid, true) → onSuccess()
    // -----------------------------------------------------------------------

    @Test
    public void login_validCredentials_updatesOnlineStatusAndCallsOnSuccess() {
        FirebaseUser mockUser = mock(FirebaseUser.class);
        when(mockUser.getUid()).thenReturn("login-uid-007");

        AuthResult mockAuthResult = mock(AuthResult.class);
        when(mockAuthResult.getUser()).thenReturn(mockUser);

        Task<AuthResult> mockTask = buildSuccessTask(mockAuthResult);
        when(mockAuth.signInWithEmailAndPassword(eq("user@example.com"), eq("pass1234")))
                .thenReturn(mockTask);

        // Mock updateOnlineStatus trả về thành công
        doAnswer(inv -> {
            UserRepository.OnCompleteCallback cb = inv.getArgument(2);
            cb.onSuccess();
            return null;
        }).when(mockUserRepository).updateOnlineStatus(
                eq("login-uid-007"), eq(true), any(UserRepository.OnCompleteCallback.class));

        repository.login("user@example.com", "pass1234", mockCallback);

        // Phải gọi updateOnlineStatus với uid đúng và isOnline=true
        verify(mockUserRepository).updateOnlineStatus(
                eq("login-uid-007"), eq(true), any(UserRepository.OnCompleteCallback.class));
        verify(mockCallback).onSuccess();
        verify(mockCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC8: Login — Sai thông tin đăng nhập → onError với message tiếng Việt
    // -----------------------------------------------------------------------

    @Test
    public void login_invalidCredentials_callsOnErrorWithVietnameseMessage() {
        FirebaseAuthInvalidCredentialsException exception =
                mock(FirebaseAuthInvalidCredentialsException.class);

        Task<AuthResult> mockTask = buildFailureTask(exception);
        when(mockAuth.signInWithEmailAndPassword(anyString(), anyString()))
                .thenReturn(mockTask);

        repository.login("user@example.com", "wrongpass", mockCallback);

        verify(mockCallback).onError("Thông tin đăng nhập không hợp lệ.");
        verify(mockCallback, never()).onSuccess();
        // Firestore không được gọi khi Auth thất bại
        verify(mockUserRepository, never()).updateOnlineStatus(anyString(), eq(true), any());
    }

    // -----------------------------------------------------------------------
    // TC9: Login — Email null → onError() ngay, Firebase không được gọi
    // -----------------------------------------------------------------------

    @Test
    public void login_nullEmail_callsOnErrorWithoutCallingFirebase() {
        repository.login(null, "pass1234", mockCallback);

        verify(mockCallback).onError("Email không được để trống.");
        verify(mockCallback, never()).onSuccess();
        verify(mockAuth, never()).signInWithEmailAndPassword(anyString(), anyString());
    }

    // -----------------------------------------------------------------------
    // TC10: Login — Password rỗng → onError() ngay, Firebase không được gọi
    // -----------------------------------------------------------------------

    @Test
    public void login_emptyPassword_callsOnErrorWithoutCallingFirebase() {
        repository.login("user@example.com", "", mockCallback);

        verify(mockCallback).onError("Mật khẩu không được để trống.");
        verify(mockCallback, never()).onSuccess();
        verify(mockAuth, never()).signInWithEmailAndPassword(anyString(), anyString());
    }

    // -----------------------------------------------------------------------
    // TC11: changePassword — Happy Path: re-auth thành công + updatePassword thành công
    // -----------------------------------------------------------------------

    @Test
    public void changePassword_validCredentials_callsOnSuccess() {
        FirebaseUser mockUser = mock(FirebaseUser.class);
        when(mockUser.getEmail()).thenReturn("user@example.com");
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);

        // Mock reauthenticate thành công
        AuthCredential mockCredential = mock(AuthCredential.class);
        Task<Void> mockReauthTask = buildVoidSuccessTask();
        when(mockUser.reauthenticate(mockCredential)).thenReturn(mockReauthTask);

        // Mock updatePassword thành công
        Task<Void> mockUpdateTask = buildVoidSuccessTask();
        when(mockUser.updatePassword("NewPass@123")).thenReturn(mockUpdateTask);

        // Dùng anonymous subclass để bypass EmailAuthProvider.getCredential() (Android API)
        FirebaseAuthRepository testRepo = new FirebaseAuthRepository(mockAuth, mockUserRepository) {
            @Override
            protected com.google.firebase.auth.AuthCredential getEmailCredential(String email, String password) {
                return mockCredential;
            }
        };

        testRepo.changePassword("OldPass@123", "NewPass@123", mockCallback);

        verify(mockCallback).onSuccess();
        verify(mockCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC12: changePassword — newPassword < 6 ký tự → onError ngay, Firebase không gọi
    // -----------------------------------------------------------------------

    @Test
    public void changePassword_shortNewPassword_callsOnErrorWithoutCallingFirebase() {
        FirebaseUser mockUser = mock(FirebaseUser.class);
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);

        repository.changePassword("OldPass@123", "12345", mockCallback);

        verify(mockCallback).onError("Mật khẩu phải có ít nhất 6 ký tự.");
        verify(mockCallback, never()).onSuccess();
        // Firebase reauthenticate không được gọi
        verify(mockUser, never()).reauthenticate(any());
    }

    // -----------------------------------------------------------------------
    // TC13: changePassword — currentPassword sai → reauthenticate fail → onError tiếng Việt
    // -----------------------------------------------------------------------

    @Test
    public void changePassword_wrongCurrentPassword_callsOnErrorWithVietnameseMessage() {
        FirebaseUser mockUser = mock(FirebaseUser.class);
        when(mockUser.getEmail()).thenReturn("user@example.com");
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);

        AuthCredential mockCredential = mock(AuthCredential.class);
        FirebaseAuthInvalidCredentialsException exception =
                mock(FirebaseAuthInvalidCredentialsException.class);

        Task<Void> mockReauthTask = buildVoidFailureTask(exception);
        when(mockUser.reauthenticate(mockCredential)).thenReturn(mockReauthTask);

        // Dùng anonymous subclass để bypass EmailAuthProvider.getCredential() (Android API)
        FirebaseAuthRepository testRepo = new FirebaseAuthRepository(mockAuth, mockUserRepository) {
            @Override
            protected com.google.firebase.auth.AuthCredential getEmailCredential(String email, String password) {
                return mockCredential;
            }
        };

        testRepo.changePassword("WrongPass", "NewPass@123", mockCallback);

        verify(mockCallback).onError("Thông tin đăng nhập không hợp lệ.");
        verify(mockCallback, never()).onSuccess();
        // updatePassword không được gọi khi re-auth fail
        verify(mockUser, never()).updatePassword(anyString());
    }

    // -----------------------------------------------------------------------
    // TC14: changePassword — User chưa đăng nhập → onError("Chưa đăng nhập")
    // -----------------------------------------------------------------------

    @Test
    public void changePassword_userNotLoggedIn_callsOnErrorNotLoggedIn() {
        when(mockAuth.getCurrentUser()).thenReturn(null);

        repository.changePassword("OldPass@123", "NewPass@123", mockCallback);

        verify(mockCallback).onError("Chưa đăng nhập");
        verify(mockCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC15: deleteAccount — Happy Path: re-auth OK → Firestore xóa OK → Auth xóa OK
    // -----------------------------------------------------------------------

    @Test
    public void deleteAccount_validCredentials_deletesFirestoreThenAuthAndCallsOnSuccess() {
        FirebaseUser mockUser = mock(FirebaseUser.class);
        when(mockUser.getEmail()).thenReturn("user@example.com");
        when(mockUser.getUid()).thenReturn("delete-uid-001");
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);

        AuthCredential mockCredential = mock(AuthCredential.class);
        Task<Void> reauthTask = buildVoidSuccessTask();
        Task<Void> deleteTask = buildVoidSuccessTask();
        when(mockUser.reauthenticate(mockCredential)).thenReturn(reauthTask);
        when(mockUser.delete()).thenReturn(deleteTask);

        doAnswer(inv -> {
            UserRepository.OnCompleteCallback cb = inv.getArgument(1);
            cb.onSuccess();
            return null;
        }).when(mockUserRepository).deleteUser(
                eq("delete-uid-001"), any(UserRepository.OnCompleteCallback.class));

        FirebaseAuthRepository testRepo = new FirebaseAuthRepository(mockAuth, mockUserRepository) {
            @Override
            protected com.google.firebase.auth.AuthCredential getEmailCredential(String email, String password) {
                return mockCredential;
            }
        };

        testRepo.deleteAccount("CurrentPass@123", mockCallback);

        // Phải xóa Firestore và Auth, sau đó gọi onSuccess
        verify(mockUserRepository).deleteUser(eq("delete-uid-001"), any());
        verify(mockUser).delete();
        verify(mockCallback).onSuccess();
        verify(mockCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC16: deleteAccount — Re-auth fail → Firestore và Auth KHÔNG bị xóa
    // -----------------------------------------------------------------------

    @Test
    public void deleteAccount_wrongPassword_callsOnErrorAndDoesNotDelete() {
        FirebaseUser mockUser = mock(FirebaseUser.class);
        when(mockUser.getEmail()).thenReturn("user@example.com");
        when(mockUser.getUid()).thenReturn("delete-uid-001");
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);

        AuthCredential mockCredential = mock(AuthCredential.class);
        FirebaseAuthInvalidCredentialsException exception =
                mock(FirebaseAuthInvalidCredentialsException.class);
        Task<Void> reauthTask = buildVoidFailureTask(exception);
        when(mockUser.reauthenticate(mockCredential)).thenReturn(reauthTask);

        FirebaseAuthRepository testRepo = new FirebaseAuthRepository(mockAuth, mockUserRepository) {
            @Override
            protected com.google.firebase.auth.AuthCredential getEmailCredential(String email, String password) {
                return mockCredential;
            }
        };

        testRepo.deleteAccount("WrongPass", mockCallback);

        verify(mockCallback).onError("Thông tin đăng nhập không hợp lệ.");
        verify(mockCallback, never()).onSuccess();
        // Firestore và Auth user KHÔNG được xóa
        verify(mockUserRepository, never()).deleteUser(anyString(), any());
        verify(mockUser, never()).delete();
    }

    // -----------------------------------------------------------------------
    // TC17: deleteAccount — User chưa đăng nhập → onError("Chưa đăng nhập")
    // -----------------------------------------------------------------------

    @Test
    public void deleteAccount_userNotLoggedIn_callsOnErrorNotLoggedIn() {
        when(mockAuth.getCurrentUser()).thenReturn(null);

        repository.deleteAccount("AnyPass@123", mockCallback);

        verify(mockCallback).onError("Chưa đăng nhập");
        verify(mockCallback, never()).onSuccess();
        verify(mockUserRepository, never()).deleteUser(anyString(), any());
    }

    // -----------------------------------------------------------------------
    // TC18: deleteAccount — Firestore delete fail → Auth user KHÔNG bị xóa
    // -----------------------------------------------------------------------

    @Test
    public void deleteAccount_firestoreDeleteFails_doesNotDeleteAuthUser() {
        FirebaseUser mockUser = mock(FirebaseUser.class);
        when(mockUser.getEmail()).thenReturn("user@example.com");
        when(mockUser.getUid()).thenReturn("delete-uid-001");
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);

        AuthCredential mockCredential = mock(AuthCredential.class);
        Task<Void> reauthTask = buildVoidSuccessTask();
        when(mockUser.reauthenticate(mockCredential)).thenReturn(reauthTask);

        // Firestore xóa thất bại
        doAnswer(inv -> {
            UserRepository.OnCompleteCallback cb = inv.getArgument(1);
            cb.onError("Lỗi kết nối Firestore");
            return null;
        }).when(mockUserRepository).deleteUser(
                eq("delete-uid-001"), any(UserRepository.OnCompleteCallback.class));

        FirebaseAuthRepository testRepo = new FirebaseAuthRepository(mockAuth, mockUserRepository) {
            @Override
            protected com.google.firebase.auth.AuthCredential getEmailCredential(String email, String password) {
                return mockCredential;
            }
        };

        testRepo.deleteAccount("CurrentPass@123", mockCallback);

        verify(mockCallback).onError("Lỗi kết nối Firestore");
        verify(mockCallback, never()).onSuccess();
        // Auth user KHÔNG được xóa vì Firestore chưa xóa xong
        verify(mockUser, never()).delete();
    }

    // -----------------------------------------------------------------------
    // Helper: tạo mock Task thành công
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private <T> Task<T> buildSuccessTask(T result) {
        Task<T> mockTask = mock(Task.class);
        doAnswer(inv -> {
            ((OnSuccessListener<T>) inv.getArgument(0)).onSuccess(result);
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(OnSuccessListener.class));
        when(mockTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockTask);
        return mockTask;
    }

    // -----------------------------------------------------------------------
    // Helper: tạo mock Task thất bại
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private <T> Task<T> buildFailureTask(Exception exception) {
        Task<T> mockTask = mock(Task.class);
        when(mockTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockTask);
        doAnswer(inv -> {
            ((OnFailureListener) inv.getArgument(0)).onFailure(exception);
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(OnFailureListener.class));
        return mockTask;
    }

    // -----------------------------------------------------------------------
    // Helper: tạo mock Task<Void> thành công
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Task<Void> buildVoidSuccessTask() {
        Task<Void> mockTask = mock(Task.class);
        doAnswer(inv -> {
            ((OnSuccessListener<Void>) inv.getArgument(0)).onSuccess(null);
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(OnSuccessListener.class));
        when(mockTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockTask);
        return mockTask;
    }

    // -----------------------------------------------------------------------
    // Helper: tạo mock Task<Void> thất bại
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Task<Void> buildVoidFailureTask(Exception exception) {
        Task<Void> mockTask = mock(Task.class);
        when(mockTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockTask);
        doAnswer(inv -> {
            ((OnFailureListener) inv.getArgument(0)).onFailure(exception);
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(OnFailureListener.class));
        return mockTask;
    }
}
