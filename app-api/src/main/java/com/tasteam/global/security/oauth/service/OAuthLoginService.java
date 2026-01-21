package com.tasteam.global.security.oauth.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.entity.oauth.MemberOAuthAccount;
import com.tasteam.domain.member.repository.MemberOAuthAccountRepository;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.global.security.common.util.NicknameGenerator;
import com.tasteam.global.security.oauth.dto.CustomOAuthUserDetails;
import com.tasteam.global.security.oauth.provider.OAuthUserInfo;
import com.tasteam.global.security.oauth.provider.OAuthUserInfoFactory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OAuthLoginService extends DefaultOAuth2UserService {

	private final MemberRepository memberRepository;
	private final MemberOAuthAccountRepository memberOAuthAccountRepository;

	@Override
	@Transactional
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = super.loadUser(userRequest);
		String registrationId = userRequest.getClientRegistration().getRegistrationId();

		OAuthUserInfo userInfo = OAuthUserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

		MemberOAuthAccount oAuthAccount = memberOAuthAccountRepository
			.findByProviderAndProviderUserId(userInfo.getProvider(), userInfo.getId())
			.orElseGet(() -> registerUser(userInfo));

		Member member = oAuthAccount.getMember();
		member.loginSuccess();

		return new CustomOAuthUserDetails(member.getId(), member.getRole().name());
	}

	private MemberOAuthAccount registerUser(OAuthUserInfo userInfo) {
		String email = userInfo.getEmail();
		String name = NicknameGenerator.fromName(userInfo.getName());

		Member member = Member.create(email, name);
		memberRepository.save(member);

		MemberOAuthAccount oAuthAccount = MemberOAuthAccount.create(
			userInfo.getProvider(),
			userInfo.getId(),
			userInfo.getEmail(),
			member);
		return memberOAuthAccountRepository.save(oAuthAccount);
	}
}
