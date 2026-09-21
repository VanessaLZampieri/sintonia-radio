package br.com.sintonia.security;

import br.com.sintonia.user.User;
import br.com.sintonia.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SintoniaOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;
    private final UserRepository userRepository;

    @Autowired
    public SintoniaOAuth2UserService(UserRepository userRepository) {
        this(userRepository, new OidcUserService());
    }

    SintoniaOAuth2UserService(UserRepository userRepository,
                              OAuth2UserService<OidcUserRequest, OidcUser> delegate) {
        this.userRepository = userRepository;
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = delegate.loadUser(userRequest);

        String googleId = oidcUser.getSubject();
        String name = oidcUser.getAttribute("name");
        String email = oidcUser.getAttribute("email");
        String avatarUrl = oidcUser.getAttribute("picture");

        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> userRepository.save(new User(googleId, name, email, avatarUrl)));
        user.updateProfile(name, email, avatarUrl);

        return new SintoniaOAuth2User(oidcUser, user.getId());
    }
}
