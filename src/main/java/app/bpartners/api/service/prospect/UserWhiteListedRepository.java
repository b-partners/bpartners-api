package app.bpartners.api.service.prospect;

import app.bpartners.api.model.UserWhiteListed;
import org.springframework.data.repository.Repository;

interface UserWhiteListedRepository extends Repository<UserWhiteListed, String> {}
