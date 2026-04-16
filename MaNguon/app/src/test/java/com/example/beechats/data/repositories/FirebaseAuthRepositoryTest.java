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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
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
}
