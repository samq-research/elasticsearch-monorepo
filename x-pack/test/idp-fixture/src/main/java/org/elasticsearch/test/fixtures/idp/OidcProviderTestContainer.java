/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.test.fixtures.idp;

import org.elasticsearch.test.fixtures.testcontainers.DockerEnvironmentAwareTestContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;

public final class OidcProviderTestContainer extends DockerEnvironmentAwareTestContainer {

    private static final int PORT = 8080;

    /**
     * for packer caching only
     * */
    protected OidcProviderTestContainer() {
        this(Network.newNetwork());
    }

    public OidcProviderTestContainer(Network network) {
        super(
            new ImageFromDockerfile("es-oidc-provider-fixture").withFileFromClasspath("oidc/setup.sh", "/oidc/setup.sh")
                // we cannot make use of docker file builder
                // as it does not support multi-stage builds
                .withFileFromClasspath("Dockerfile", "oidc/Dockerfile")
        );
        withNetworkAliases("oidc-provider");
        withNetwork(network);
        addExposedPort(PORT);
    }

    @Override
    public void start() {
        super.start();
        copyFileToContainer(
            Transferable.of(
                "op.issuer=http://127.0.0.1:"
                    + getMappedPort(PORT)
                    + "/c2id\n"
                    + "op.authz.endpoint=http://127.0.0.1:"
                    + getMappedPort(PORT)
                    + "/c2id-login/\n"
                    + "op.reg.apiAccessTokenSHA256=d1c4fa70d9ee708d13cfa01daa0e060a05a2075a53c5cc1ad79e460e96ab5363\n"
                    + "jose.jwkSer=RnVsbCBrZXk6CnsKICAia2V5cyI6IFsKICAgIHsKICAgICAgInAiOiAiLXhhN2d2aW5tY3N3QXU3Vm1mV2loZ2o3U3gzUzhmd2dFSTdMZEVveW5FU1RzcElaeUY5aHc0NVhQZmI5VHlpbzZsOHZTS0F5RmU4T2lOalpkNE1Ra0ttYlJzTmxxR1Y5VlBoWF84UG1JSm5mcGVhb3E5YnZfU0k1blZHUl9zYUUzZE9sTEE2VWpaS0lsRVBNb0ZuRlZCMUFaUU9qQlhRRzZPTDg2eDZ2NHMwIiwKICAgICAgImt0eSI6ICJSU0EiLAogICAgICAicSI6ICJ2Q3pDQUlpdHV0MGx1V0djQloyLUFabURLc1RxNkkxcUp0RmlEYkIyZFBNQVlBNldOWTdaWEZoVWxsSjJrT2ZELWdlYjlkYkN2ODBxNEwyajVZSjZoOTBUc1NRWWVHRlljN1lZMGdCMU5VR3l5cXctb29QN0EtYlJmMGI3b3I4ajZJb0hzQTZKa2JranN6c3otbkJ2U2RmUURlZkRNSVc3Ni1ZWjN0c2hsY2MiLAogICAgICAiZCI6ICJtbFBOcm1zVVM5UmJtX1I5SElyeHdmeFYzZnJ2QzlaQktFZzRzc1ZZaThfY09lSjV2U1hyQV9laEtwa2g4QVhYaUdWUGpQbVlyd29xQzFVUksxUkZmLVg0dG10emV2OUVHaU12Z0JCaEF5RkdTSUd0VUNla2x4Q2dhb3BpMXdZSU1Bd0M0STZwMUtaZURxTVNCWVZGeHA5ZWlJZ2pwb05JbV9lR3hXUUs5VHNnYmk5T3lyc1VqaE9KLVczN2JVMEJWUU56UXpxODhCcGxmNzM3VmV1dy1FeDZaMk1iWXR3SWdfZ0JVb0JEZ0NrZkhoOVE4MElYcEZRV0x1RzgwenFrdkVwTHZ0RWxLbDRvQ3BHVnBjcmFUOFNsOGpYc3FDT1k0dnVRT19LRVUzS2VPNUNJbHd4eEhJYXZjQTE5cHFpSWJ5cm1LbThxS0ZEWHluUFJMSGFNZ1EiLAogICAgICAiZSI6ICJBUUFCIiwKICAgICAgImtpZCI6ICJyc2EzODRfMjA0OCIsCiAgICAgICJxaSI6ICJzMldTamVrVDl3S2JPbk9neGNoaDJPY3VubzE2Y20wS281Z3hoUWJTdVMyMldfUjJBR2ZVdkRieGF0cTRLakQ3THo3X1k2TjdTUkwzUVpudVhoZ1djeXgyNGhrUGppQUZLNmlkYVZKQzJqQmgycEZTUDVTNXZxZ0lsME12eWY4NjlwdkN4S0NzaGRKMGdlRWhveE93VkRPYXJqdTl2Zm9IQV90LWJoRlZrUnciLAogICAgICAiZHAiOiAiQlJhQTFqYVRydG9mTHZBSUJBYW1OSEVhSm51RU9zTVJJMFRCZXFuR1BNUm0tY2RjSG1OUVo5WUtqb2JpdXlmbnhGZ0piVDlSeElBRG0ySkpoZEp5RTN4Y1dTSzhmSjBSM1Jick1aT1dwako0QmJTVzFtU1VtRnlKTGxib3puRFhZR2RaZ1hzS0o1UkFrRUNQZFBCY3YwZVlkbk9NYWhfZndfaFZoNjRuZ2tFIiwKICAgICAgImFsZyI6ICJSU0EzODQiLAogICAgICAiZHEiOiAiUFJoVERKVlR3cDNXaDZfWFZrTjIwMUlpTWhxcElrUDN1UTYyUlRlTDNrQ2ZXSkNqMkZPLTRxcVRIQk0tQjZJWUVPLXpoVWZyQnhiMzJ1djNjS2JDWGFZN3BJSFJxQlFEQWQ2WGhHYzlwc0xqNThXd3VGY2RncERJYUFpRjNyc3NUMjJ4UFVvYkJFTVdBalV3bFJrNEtNTjItMnpLQk5FR3lIcDIzOUpKdnpVIiwKICAgICAgIm4iOiAidUpDWDVDbEZpM0JnTXBvOWhRSVZ2SDh0Vi1jLTVFdG5OeUZxVm91R3NlNWwyUG92MWJGb0tsRllsU25YTzNWUE9KRWR3azNDdl9VT0UtQzlqZERYRHpvS3Z4RURaTVM1TDZWMFpIVEJoNndIOV9iN3JHSlBxLV9RdlNkejczSzZxbHpGaUtQamRvdTF6VlFYTmZfblBZbnRnQkdNRUtBc1pRNGp0cWJCdE5lV0h0MF9UM001cEktTV9KNGVlRWpCTW95TkZuU2ExTEZDVmZRNl9YVnpjelp1TlRGMlh6UmdRWkFmcmJGRXZ6eXR1TzVMZTNTTXFrUUFJeDhFQmkwYXVlRUNqNEQ4cDNVNXFVRG92NEF2VnRJbUZlbFJvb1pBMHJtVW1KRHJ4WExrVkhuVUpzaUF6ZW9TLTNBSnV1bHJkMGpuNjJ5VjZHV2dFWklZMVNlZVd3IgogICAgfQogIF0KfQo\n"
                    + "op.authz.alwaysPromptForConsent=true\n"
                    + "op.authz.alwaysPromptForAuth=true"
            ),
            "config/c2id/override.properties"
        );
    }

    public String getC2OPUrl() {
        return "http://127.0.0.1:" + getMappedPort(PORT);
    }

    public String getC2IssuerUrl() {
        return getC2OPUrl() + "/c2id";
    }

}
