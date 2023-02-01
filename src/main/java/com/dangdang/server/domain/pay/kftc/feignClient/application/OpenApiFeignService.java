package com.dangdang.server.domain.pay.kftc.feignClient.application;

import static com.dangdang.server.global.exception.ExceptionCode.OPEN_OAUTH_NOT_COMPLETE;

import com.dangdang.server.domain.pay.daangnpay.domain.connectionAccount.exception.EmptyResultException;
import com.dangdang.server.domain.pay.daangnpay.domain.payMember.domain.entity.PayMember;
import com.dangdang.server.domain.pay.kftc.OpenBankingService;
import com.dangdang.server.domain.pay.kftc.feignClient.OpenApiFeignClient;
import com.dangdang.server.domain.pay.kftc.feignClient.domain.OpenBankingMember;
import com.dangdang.server.domain.pay.kftc.feignClient.domain.OpenBankingMemberRepository;
import com.dangdang.server.domain.pay.kftc.feignClient.dto.AuthTokenRequestProperties;
import com.dangdang.server.domain.pay.kftc.feignClient.dto.GetAuthTokenRequest;
import com.dangdang.server.domain.pay.kftc.feignClient.dto.GetAuthTokenResponse;
import com.dangdang.server.domain.pay.kftc.feignClient.dto.GetUserMeResponse;
import com.dangdang.server.domain.pay.kftc.openBankingFacade.dto.OpenBankingDepositRequest;
import com.dangdang.server.domain.pay.kftc.openBankingFacade.dto.OpenBankingInquiryReceiveRequest;
import com.dangdang.server.domain.pay.kftc.openBankingFacade.dto.OpenBankingInquiryReceiveResponse;
import com.dangdang.server.domain.pay.kftc.openBankingFacade.dto.OpenBankingResponse;
import com.dangdang.server.domain.pay.kftc.openBankingFacade.dto.OpenBankingWithdrawRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Profile("external")
@Service
public class OpenApiFeignService implements OpenBankingService {

  private final OpenBankingMemberRepository openBankingMemberRepository;
  private final OpenApiFeignClient openApiFeignClient;
  private final AuthTokenRequestProperties authTokenRequestProperties;

  public OpenApiFeignService(OpenBankingMemberRepository openBankingMemberRepository,
      OpenApiFeignClient openApiFeignClient,
      AuthTokenRequestProperties authTokenRequestProperties) {
    this.openBankingMemberRepository = openBankingMemberRepository;
    this.openApiFeignClient = openApiFeignClient;
    this.authTokenRequestProperties = authTokenRequestProperties;
  }

  /**
   * 가입 (토큰 발급 전)
   */
  @Override
  public void createOpenBankingMemberFromState(String state, PayMember payMember) {
    OpenBankingMember openBankingMember = openBankingMemberRepository.findByPayMemberId(
        payMember.getId()).orElseGet(() -> new OpenBankingMember(state, payMember));

    if (!openBankingMember.getState().equals(state)) {
      openBankingMember.updateStateAndPayMember(openBankingMember);
    } else {
      openBankingMemberRepository.save(openBankingMember);
    }
  }

  /**
   * 토큰 발급
   */
  @Transactional
  public GetAuthTokenResponse getAuthToken(GetAuthTokenRequest getAuthTokenRequest) {
    OpenBankingMember openBankingMember = openBankingMemberRepository.findByState(
            getAuthTokenRequest.state())
        .orElseThrow(() -> new EmptyResultException(OPEN_OAUTH_NOT_COMPLETE));
    OpenBankingMember updateOpenBankingMember = GetAuthTokenRequest.to(getAuthTokenRequest);
    openBankingMember.updateCode(updateOpenBankingMember);

    GetAuthTokenResponse getAuthTokenResponse = openApiFeignClient.getAuthorizeToken(
        getAuthTokenRequest.code(), authTokenRequestProperties.getId(),
        authTokenRequestProperties.getSecret(),
        authTokenRequestProperties.getUri(), authTokenRequestProperties.getGrantType());
    OpenBankingMember updateTokenAndUserSeqNo = GetAuthTokenResponse.to(getAuthTokenResponse);
    openBankingMember.updateTokenAndSeqNo(updateTokenAndUserSeqNo);
    openBankingMemberRepository.save(openBankingMember);

    return getAuthTokenResponse;
  }

  /**
   * 사용자 정보 조회
   */
  public GetUserMeResponse getUserMeResponse(String token, String user_seq_no) {
    return openApiFeignClient.getUserInfo(token, user_seq_no);
  }

  @Override
  public OpenBankingResponse deposit(OpenBankingDepositRequest openBankingDepositRequest) {
    return null;
  }

  @Override
  public OpenBankingResponse withdraw(OpenBankingWithdrawRequest openBankingWithdrawRequest) {
    return null;
  }

  @Override
  public OpenBankingInquiryReceiveResponse inquiryReceive(
      OpenBankingInquiryReceiveRequest openBankingInquiryReceiveRequest) {
    return null;
  }
}