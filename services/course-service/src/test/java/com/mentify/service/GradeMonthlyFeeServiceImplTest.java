package com.mentify.service;

import com.mentify.dto.GradeMonthlyFeeRequest;
import com.mentify.entity.Grade;
import com.mentify.entity.GradeMonthlyFee;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.repository.GradeMonthlyFeeRepository;
import com.mentify.repository.GradeRepository;
import com.mentify.service.impl.GradeMonthlyFeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeMonthlyFeeServiceImplTest {

    @Mock
    private GradeMonthlyFeeRepository feeRepository;

    @Mock
    private GradeRepository gradeRepository;

    private GradeMonthlyFeeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GradeMonthlyFeeServiceImpl(feeRepository, gradeRepository);
    }

    @Test
    void createStoresMonthlyFeeForExistingGradeAndDefaultsCurrency() {
        UUID gradeId = UUID.randomUUID();
        Grade grade = grade(gradeId, "Grade 6");
        when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(grade));
        when(feeRepository.existsByGrade_Id(gradeId)).thenReturn(false);
        when(feeRepository.saveAndFlush(any(GradeMonthlyFee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(gradeId, request("2000", null));

        assertThat(response.getStatusCode()).isEqualTo(201);
        assertThat(response.getData().getGradeId()).isEqualTo(gradeId);
        assertThat(response.getData().getGradeName()).isEqualTo("Grade 6");
        assertThat(response.getData().getMonthlyFee()).isEqualByComparingTo("2000.00");
        assertThat(response.getData().getCurrencyCode()).isEqualTo("LKR");
    }

    @Test
    void createRejectsUnknownGrade() {
        UUID gradeId = UUID.randomUUID();
        when(gradeRepository.findById(gradeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(gradeId, request("2000", "LKR")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createRejectsSecondFeeForGrade() {
        UUID gradeId = UUID.randomUUID();
        when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(grade(gradeId, "Grade 8")));
        when(feeRepository.existsByGrade_Id(gradeId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(gradeId, request("3400", "LKR")))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Grade 8");
    }

    @Test
    void getAllReturnsFeeRows() {
        Grade grade = grade(UUID.randomUUID(), "Grade 6");
        GradeMonthlyFee fee = fee(grade, "2000");
        when(feeRepository.findAllByOrderByGrade_NameAsc()).thenReturn(List.of(fee));

        assertThat(service.getAll().getData()).hasSize(1);
        assertThat(service.getAll().getData().get(0).getMonthlyFee()).isEqualByComparingTo("2000");
    }

    @Test
    void getByGradeRejectsMissingFee() {
        UUID gradeId = UUID.randomUUID();
        when(feeRepository.findByGrade_Id(gradeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByGrade(gradeId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateChangesAmountAndCurrency() {
        UUID gradeId = UUID.randomUUID();
        GradeMonthlyFee fee = fee(grade(gradeId, "Grade 8"), "3400");
        when(feeRepository.findByGrade_Id(gradeId)).thenReturn(Optional.of(fee));
        when(feeRepository.save(fee)).thenReturn(fee);

        var response = service.update(gradeId, request("3500.50", "lkr"));

        assertThat(response.getData().getMonthlyFee()).isEqualByComparingTo("3500.50");
        assertThat(response.getData().getCurrencyCode()).isEqualTo("LKR");
    }

    @Test
    void deleteRemovesGradeFee() {
        UUID gradeId = UUID.randomUUID();
        GradeMonthlyFee fee = fee(grade(gradeId, "Grade 6"), "2000");
        when(feeRepository.findByGrade_Id(gradeId)).thenReturn(Optional.of(fee));

        service.delete(gradeId);

        verify(feeRepository).delete(fee);
    }

    private GradeMonthlyFeeRequest request(String amount, String currency) {
        return GradeMonthlyFeeRequest.builder()
                .monthlyFee(new BigDecimal(amount)).currencyCode(currency).build();
    }

    private Grade grade(UUID id, String name) {
        Grade grade = Grade.builder().name(name).build();
        grade.setId(id);
        return grade;
    }

    private GradeMonthlyFee fee(Grade grade, String amount) {
        return GradeMonthlyFee.builder().grade(grade).monthlyFee(new BigDecimal(amount)).currencyCode("LKR").build();
    }
}
