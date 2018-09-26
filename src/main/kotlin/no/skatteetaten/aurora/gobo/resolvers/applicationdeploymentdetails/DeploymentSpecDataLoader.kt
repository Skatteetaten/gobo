package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import com.fasterxml.jackson.databind.JsonNode
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import no.skatteetaten.aurora.gobo.security.UserService
import no.skatteetaten.aurora.utils.logLine
import no.skatteetaten.aurora.utils.time
import org.dataloader.Try
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.net.URI
import java.net.URL

@Component
class DeploymentSpecDataLoader(
    @TargetService(ServiceTypes.BOOBER) private val webClient: WebClient,
    private val userService: UserService
) : KeysDataLoader<URL, Try<DeploymentSpec>> {

    private val logger: Logger = LoggerFactory.getLogger(DeploymentSpecDataLoader::class.java)

    override fun getByKeys(keys: List<URL>): List<Try<DeploymentSpec>> {

        logger.info("Loading ${keys.size} DeploymentSpecs from boober (${keys.toSet().size} unique)")

        val token = userService.getToken()

        val sw = StopWatch()
        val specs: List<Try<DeploymentSpec>> = sw.time("Fetch ${keys.size} DeploymentSpecs") {
            keys.map {
                Try.tryCall {

                    val uri = URI(it.toString().replace("http://boober/api/", "http://boober-aurora.utv.paas.skead.no/"))
                    logger.debug("Loading spec from url=$uri")
                    val spec: DeploymentSpecResponse? = try {
                        webClient
                                .get()
                                .uri(uri)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                                .retrieve()
                                .bodyToMono<DeploymentSpecResponse>().block()!!
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }

                    val jsonRepresentation = spec?.items?.get(0)?.toString() ?: ""
                    DeploymentSpec(jsonRepresentation = jsonRepresentation)
                }
            }
        }

        logger.info(sw.logLine)
        return specs
    }
}

data class DeploymentSpecResponse(val success: Boolean, val message: String, val items: List<JsonNode>, val count: Int)

@Language("JSON")
val json = """{
  "success": true,
  "message": "OK",
  "items": [
    {
      "applicationDeploymentRef": {
        "source": "static",
        "value": "it/elsaer-server",
        "sources": [
          {
            "name": "static",
            "value": "it/elsaer-server"
          }
        ]
      },
      "configVersion": {
        "source": "static",
        "value": "master",
        "sources": [
          {
            "name": "static",
            "value": "master"
          }
        ]
      },
      "schemaVersion": {
        "source": "about.json",
        "value": "v1",
        "sources": [
          {
            "name": "about.json",
            "value": "v1"
          }
        ]
      },
      "type": {
        "source": "about.json",
        "value": "deploy",
        "sources": [
          {
            "name": "about.json",
            "value": "deploy"
          }
        ]
      },
      "applicationPlatform": {
        "source": "default",
        "value": "java",
        "sources": [
          {
            "name": "default",
            "value": "java"
          }
        ]
      },
      "affiliation": {
        "source": "about.json",
        "value": "safir",
        "sources": [
          {
            "name": "about.json",
            "value": "safir"
          }
        ]
      },
      "cluster": {
        "source": "it/about.json",
        "value": "utv",
        "sources": [
          {
            "name": "it/about.json",
            "value": "utv"
          }
        ]
      },
      "permissions": {
        "admin": {
          "source": "about.json",
          "value": "APP_SAFIR_drift APP_SAFIR_ref APP_SAFIR_utv",
          "sources": [
            {
              "name": "about.json",
              "value": "APP_SAFIR_drift APP_SAFIR_ref APP_SAFIR_utv"
            }
          ]
        }
      },
      "envName": {
        "source": "folderName",
        "value": "it",
        "sources": [
          {
            "name": "folderName",
            "value": "it"
          }
        ]
      },
      "name": {
        "source": "fileName",
        "value": "elsaer-server",
        "sources": [
          {
            "name": "fileName",
            "value": "elsaer-server"
          }
        ]
      },
      "splunkIndex": {
        "source": "about.json",
        "value": "safir-test",
        "sources": [
          {
            "name": "about.json",
            "value": "safir-test"
          }
        ]
      },
      "certificate": {
        "commonName": {
          "source": "elsaer-server.json",
          "value": "ske.fastsetting.avgift.saeravgift.elsaer-server",
          "sources": [
            {
              "name": "elsaer-server.json",
              "value": "ske.fastsetting.avgift.saeravgift.elsaer-server"
            }
          ]
        },
        "source": "default",
        "value": false,
        "sources": [
          {
            "name": "default",
            "value": false
          }
        ]
      },
      "database": {
        "source": "elsaer-server.json",
        "value": true,
        "sources": [
          {
            "name": "default",
            "value": false
          },
          {
            "name": "elsaer-server.json",
            "value": true
          }
        ]
      },
      "prometheus": {
        "source": "default",
        "value": true,
        "sources": [
          {
            "name": "default",
            "value": true
          }
        ],
        "path": {
          "source": "default",
          "value": "/prometheus",
          "sources": [
            {
              "name": "default",
              "value": "/prometheus"
            }
          ]
        },
        "port": {
          "source": "default",
          "value": 8081,
          "sources": [
            {
              "name": "default",
              "value": 8081
            }
          ]
        }
      },
      "management": {
        "source": "elsaer-server.json",
        "value": false,
        "sources": [
          {
            "name": "default",
            "value": true
          },
          {
            "name": "elsaer-server.json",
            "value": false
          }
        ],
        "path": {
          "source": "default",
          "value": "actuator",
          "sources": [
            {
              "name": "default",
              "value": "actuator"
            }
          ]
        },
        "port": {
          "source": "default",
          "value": "8081",
          "sources": [
            {
              "name": "default",
              "value": "8081"
            }
          ]
        }
      },
      "deployStrategy": {
        "type": {
          "source": "default",
          "value": "rolling",
          "sources": [
            {
              "name": "default",
              "value": "rolling"
            }
          ]
        },
        "timeout": {
          "source": "default",
          "value": 180,
          "sources": [
            {
              "name": "default",
              "value": 180
            }
          ]
        }
      },
      "webseal": {
        "source": "default",
        "value": false,
        "sources": [
          {
            "name": "default",
            "value": false
          }
        ]
      },
      "artifactId": {
        "source": "elsaer-server.json",
        "value": "elsaer-server-leveransepakke",
        "sources": [
          {
            "name": "fileName",
            "value": "elsaer-server"
          },
          {
            "name": "elsaer-server.json",
            "value": "elsaer-server-leveransepakke"
          }
        ]
      },
      "groupId": {
        "source": "elsaer-server.json",
        "value": "ske.fastsetting.avgift.saeravgift.elsaer",
        "sources": [
          {
            "name": "elsaer-server.json",
            "value": "ske.fastsetting.avgift.saeravgift.elsaer"
          }
        ]
      },
      "version": {
        "source": "it/elsaer-server.json",
        "value": "latest",
        "sources": [
          {
            "name": "elsaer-server.json",
            "value": "latest"
          },
          {
            "name": "it/elsaer-server.json",
            "value": "latest"
          }
        ]
      },
      "resources": {
        "cpu": {
          "min": {
            "source": "default",
            "value": "100m",
            "sources": [
              {
                "name": "default",
                "value": "100m"
              }
            ]
          },
          "max": {
            "source": "default",
            "value": "2000m",
            "sources": [
              {
                "name": "default",
                "value": "2000m"
              }
            ]
          }
        },
        "memory": {
          "min": {
            "source": "default",
            "value": "128Mi",
            "sources": [
              {
                "name": "default",
                "value": "128Mi"
              }
            ]
          },
          "max": {
            "source": "elsaer-server.json",
            "value": "2Gi",
            "sources": [
              {
                "name": "default",
                "value": "512Mi"
              },
              {
                "name": "elsaer-server.json",
                "value": "2Gi"
              }
            ]
          }
        }
      },
      "replicas": {
        "source": "default",
        "value": 1,
        "sources": [
          {
            "name": "default",
            "value": 1
          }
        ]
      },
      "readiness": {
        "source": "default",
        "value": true,
        "sources": [
          {
            "name": "default",
            "value": true
          }
        ],
        "port": {
          "source": "default",
          "value": 8080,
          "sources": [
            {
              "name": "default",
              "value": 8080
            }
          ]
        },
        "delay": {
          "source": "default",
          "value": 10,
          "sources": [
            {
              "name": "default",
              "value": 10
            }
          ]
        },
        "timeout": {
          "source": "default",
          "value": 1,
          "sources": [
            {
              "name": "default",
              "value": 1
            }
          ]
        }
      },
      "liveness": {
        "source": "default",
        "value": false,
        "sources": [
          {
            "name": "default",
            "value": false
          }
        ],
        "port": {
          "source": "default",
          "value": 8080,
          "sources": [
            {
              "name": "default",
              "value": 8080
            }
          ]
        },
        "delay": {
          "source": "default",
          "value": 10,
          "sources": [
            {
              "name": "default",
              "value": 10
            }
          ]
        },
        "timeout": {
          "source": "default",
          "value": 1,
          "sources": [
            {
              "name": "default",
              "value": 1
            }
          ]
        }
      },
      "debug": {
        "source": "default",
        "value": false,
        "sources": [
          {
            "name": "default",
            "value": false
          }
        ]
      },
      "pause": {
        "source": "default",
        "value": false,
        "sources": [
          {
            "name": "default",
            "value": false
          }
        ]
      },
      "alarm": {
        "source": "default",
        "value": true,
        "sources": [
          {
            "name": "default",
            "value": true
          }
        ]
      },
      "toxiproxy": {
        "source": "default",
        "value": false,
        "sources": [
          {
            "name": "default",
            "value": false
          }
        ],
        "version": {
          "source": "default",
          "value": "2.1.3",
          "sources": [
            {
              "name": "default",
              "value": "2.1.3"
            }
          ]
        }
      },
      "config": {
        "OPPSLAGSTJENESTE_URL": {
          "source": "about.json",
          "value": "http://oppslagstjeneste/registry/tjeneste",
          "sources": [
            {
              "name": "about.json",
              "value": "http://oppslagstjeneste/registry/tjeneste"
            }
          ]
        },
        "JWT_VALIDERING_URL": {
          "source": "about.json",
          "value": "http://jwt.preprod.skead.no",
          "sources": [
            {
              "name": "about.json",
              "value": "http://jwt.preprod.skead.no"
            }
          ]
        },
        "IPLATTFORM_STS_USERNAME": {
          "source": "about.json",
          "value": "SAFIR",
          "sources": [
            {
              "name": "about.json",
              "value": "SAFIR"
            }
          ]
        },
        "APP_SAERAVGIFTSKATALOG_URL": {
          "source": "about.json",
          "value": "http://saeravgiftskatalog",
          "sources": [
            {
              "name": "about.json",
              "value": "http://saeravgiftskatalog"
            }
          ]
        },
        "FDOK_DOKUMENTKATEGORIID": {
          "source": "elsaer-server.json",
          "value": "SAER_TILBAKEMELDING",
          "sources": [
            {
              "name": "elsaer-server.json",
              "value": "SAER_TILBAKEMELDING"
            }
          ]
        },
        "FDOK_KONTEKST": {
          "source": "elsaer-server.json",
          "value": "SAER",
          "sources": [
            {
              "name": "elsaer-server.json",
              "value": "SAER"
            }
          ]
        },
        "CONNECT_TIMEOUT": {
          "source": "elsaer-server.json",
          "value": "20000",
          "sources": [
            {
              "name": "elsaer-server.json",
              "value": "20000"
            }
          ]
        },
        "READ_TIMEOUT": {
          "source": "elsaer-server.json",
          "value": "11000",
          "sources": [
            {
              "name": "elsaer-server.json",
              "value": "11000"
            }
          ]
        },
        "REQUEST_TIMEOUT": {
          "source": "elsaer-server.json",
          "value": "20000",
          "sources": [
            {
              "name": "elsaer-server.json",
              "value": "20000"
            }
          ]
        },
        "SAKSMAPPE_SYNKRONTKALL_TIMEOUT": {
          "source": "elsaer-server.json",
          "value": "8000",
          "sources": [
            {
              "name": "elsaer-server.json",
              "value": "8000"
            }
          ]
        },
        "AD_HOST": {
          "source": "elsaer-server.json",
          "value": "ldap.skead.no",
          "sources": [
            {
              "name": "elsaer-server.json",
              "value": "ldap.skead.no"
            }
          ]
        },
        "AD_USERNAME": {
          "source": "elsaer-server.json",
          "value": "srv-avgweb-server-t",
          "sources": [
            {
              "name": "elsaer-server.json",
              "value": "srv-avgweb-server-t"
            }
          ]
        },
        "PARTBASEURL": {
          "source": "it/about.json",
          "value": "part-fk1-utv.utv.paas.skead.no",
          "sources": [
            {
              "name": "it/about.json",
              "value": "part-fk1-utv.utv.paas.skead.no"
            }
          ]
        },
        "EKSTERN_OPPSLAGSTJENESTE": {
          "source": "it/about.json",
          "value": "http://oppslagstjeneste-safir-it.utv.paas.skead.no/registry/tjeneste",
          "sources": [
            {
              "name": "it/about.json",
              "value": "http://oppslagstjeneste-safir-it.utv.paas.skead.no/registry/tjeneste"
            }
          ]
        },
        "IPLATTFORM_STS_SERVICE_URL": {
          "source": "it/about.json",
          "value": "https://int-utv.skead.no:11001/felles/sikkerhet/stsSikkerhet/v1",
          "sources": [
            {
              "name": "it/about.json",
              "value": "https://int-utv.skead.no:11001/felles/sikkerhet/stsSikkerhet/v1"
            }
          ]
        },
        "IPLATTFORM_STS_BRUKSKONTEKST": {
          "source": "it/about.json",
          "value": "DEV",
          "sources": [
            {
              "name": "it/about.json",
              "value": "DEV"
            }
          ]
        },
        "ATS_URL": {
          "source": "it/about.json",
          "value": "https://ats-it.skead.no/asm-pdp/pdp",
          "sources": [
            {
              "name": "it/about.json",
              "value": "https://ats-it.skead.no/asm-pdp/pdp"
            }
          ]
        },
        "STS_URL": {
          "source": "it/about.json",
          "value": "https://int-utv.skead.no:11101/felles/sikkerhet/stsSikkerhet/v2/utstedSaml",
          "sources": [
            {
              "name": "it/about.json",
              "value": "https://int-utv.skead.no:11101/felles/sikkerhet/stsSikkerhet/v2/utstedSaml"
            }
          ]
        },
        "APP_STS_KONVERTER_URL": {
          "source": "it/about.json",
          "value": "http://tokenhjelper-utv.app2app.intern-preprod.skead.no/tokenhjelper/req/httpheader/resp/gz/attr/keyinfo",
          "sources": [
            {
              "name": "it/about.json",
              "value": "http://tokenhjelper-utv.app2app.intern-preprod.skead.no/tokenhjelper/req/httpheader/resp/gz/attr/keyinfo"
            }
          ]
        },
        "STS_REST_SERVICE_URL": {
          "source": "it/about.json",
          "value": "https://int-utv.skead.no:11101/felles/sikkerhet/stsSikkerhet/v2",
          "sources": [
            {
              "name": "it/about.json",
              "value": "https://int-utv.skead.no:11101/felles/sikkerhet/stsSikkerhet/v2"
            }
          ]
        },
        "FDOK_SENDERID": {
          "source": "it/about.json",
          "value": "srv-saer-utsend-t",
          "sources": [
            {
              "name": "it/about.json",
              "value": "srv-saer-utsend-t"
            }
          ]
        },
        "BRUKSKONTEKST_MILJO": {
          "source": "it/about.json",
          "value": "TEST",
          "sources": [
            {
              "name": "it/about.json",
              "value": "TEST"
            }
          ]
        },
        "FDOK_DOKUMENTPRODUKSJON_URL": {
          "source": "it/about.json",
          "value": "http://til0fdok-q-app01.skead.no:4080/dokumentregister-Q/DokumentproduksjonFunksjon",
          "sources": [
            {
              "name": "it/about.json",
              "value": "http://til0fdok-q-app01.skead.no:4080/dokumentregister-Q/DokumentproduksjonFunksjon"
            }
          ]
        },
        "FDOK_DOKUMENTLAGER_URL": {
          "source": "it/about.json",
          "value": "http://til0fdok-q-app01.skead.no:4080/dokumentregister-Q/DokumentlagerData",
          "sources": [
            {
              "name": "it/about.json",
              "value": "http://til0fdok-q-app01.skead.no:4080/dokumentregister-Q/DokumentlagerData"
            }
          ]
        },
        "FDOK_DOKUMENTMAL_URL": {
          "source": "it/about.json",
          "value": "http://til0fdok-q-app01.skead.no:2080/dokumentproduksjon/DokumentmalData",
          "sources": [
            {
              "name": "it/about.json",
              "value": "http://til0fdok-q-app01.skead.no:2080/dokumentproduksjon/DokumentmalData"
            }
          ]
        },
        "SKATTEFINN_URL": {
          "source": "it/about.json",
          "value": "https://ssotest.sits.no:44306/skattefinn-sit-utv/",
          "sources": [
            {
              "name": "it/about.json",
              "value": "https://ssotest.sits.no:44306/skattefinn-sit-utv/"
            }
          ]
        },
        "ALTINN_ADMINISTRASJON_URL": {
          "source": "it/elsaer-server.json",
          "value": "https://int-utv.skead.no:11001/AuthorizationExternal/AdministrationExternal.svc",
          "sources": [
            {
              "name": "it/elsaer-server.json",
              "value": "https://int-utv.skead.no:11001/AuthorizationExternal/AdministrationExternal.svc"
            }
          ]
        },
        "ALTINN_SENDERID": {
          "source": "it/elsaer-server.json",
          "value": 28033700964,
          "sources": [
            {
              "name": "it/elsaer-server.json",
              "value": 28033700964
            }
          ]
        },
        "SERVER_MODE": {
          "source": "it/elsaer-server.json",
          "value": "dev",
          "sources": [
            {
              "name": "it/elsaer-server.json",
              "value": "dev"
            }
          ]
        },
        "FNR_MODE": {
          "source": "it/elsaer-server.json",
          "value": false,
          "sources": [
            {
              "name": "it/elsaer-server.json",
              "value": false
            }
          ]
        }
      },
      "route": {
        "elsaer-server": {
          "annotations": {
            "haproxy.router.openshift.io|timeout": {
              "source": "elsaer-server.json",
              "value": "120s",
              "sources": [
                {
                  "name": "elsaer-server.json",
                  "value": "120s"
                }
              ]
            }
          }
        },
        "source": "default",
        "value": false,
        "sources": [
          {
            "name": "default",
            "value": false
          }
        ]
      },
      "routeDefaults": {
        "host": {
          "source": "default",
          "value": "@name@-@affiliation@-@env@",
          "sources": [
            {
              "name": "default",
              "value": "@name@-@affiliation@-@env@"
            }
          ]
        }
      },
      "secretVault": {
        "source": "it/about.json",
        "value": "UtvVault",
        "sources": [
          {
            "name": "it/about.json",
            "value": "UtvVault"
          }
        ]
      },
      "applicationId": {
        "source": "static",
        "value": "it/elsaer-server",
        "sources": [
          {
            "name": "static",
            "value": "it/elsaer-server"
          }
        ]
      }
    }
  ],
  "count": 1
}"""